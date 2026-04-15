package com.example.fairshare;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calculates settlement amounts based on the breakdown Map from Firestore.
 * 
 * Logic:
 * - If currentUser is the payerUid: They are owed the sum of all values in breakdown
 *   excluding their own entry (if present)
 * - If currentUser is in breakdown (and not the payer): They owe that specific value
 *   from the breakdown to the payerUid
 * 
 * This replaces the general debt simplification algorithm for direct settlement
 * calculation based on explicit split amounts.
 */
public class SettlementCalculator {

    /**
     * Represents a settlement transaction breakdown for a specific expense.
     * Shows who owes whom and how much based on the breakdown map.
     */
    public static class SettlementDetail {
        public final String expenseId;
        public final String expenseTitle;
        public final String payerUid;
        public final String payerName;
        public final double amount;
        public final String debtorUid;      // The user who owes
        public final String creditorUid;    // The user who is owed
        public final double settlementAmount;
        public final boolean settled;       // Whether this debt has been marked as settled
        public final long dateAdded;        // Timestamp of the expense creation
        public final String settledDate;    // When this specific debt was settled (null if not settled)
        public final String perspectiveText; // "You owe [Name]" or "[Name] owes you"

        public SettlementDetail(String expenseId, String expenseTitle, String payerUid, 
                               String payerName, double amount, String debtorUid, 
                               String creditorUid, double settlementAmount, boolean settled,
                               long dateAdded, String settledDate, String perspectiveText) {
            this.expenseId = expenseId;
            this.expenseTitle = expenseTitle;
            this.payerUid = payerUid;
            this.payerName = payerName;
            this.amount = amount;
            this.debtorUid = debtorUid;
            this.creditorUid = creditorUid;
            this.settlementAmount = settlementAmount;
            this.settled = settled;
            this.dateAdded = dateAdded;
            this.settledDate = settledDate;
            this.perspectiveText = perspectiveText;
        }
    }

    /**
     * Calculates itemized settlement details for all expenses in the group.
     * This method creates individual settlement objects for each debt entry from the current user's perspective.
     * 
     * @param currentUserId  The current user's UID (for filtering if needed)
     * @param expenses       List of all group expenses
     * @param memberNames    Map of uid -> display name
     * @return List of itemized settlement details
     */
    public static List<SettlementDetail> calculateSettlements(
            String currentUserId, 
            List<GroupExpense> expenses,
            Map<String, String> memberNames) {
        
        List<SettlementDetail> settlements = new ArrayList<>();
        
        if (expenses == null || expenses.isEmpty()) {
            return settlements;
        }
        
        // Flatten Expenses: Loop through every GroupExpense and create individual settlement objects
        for (GroupExpense expense : expenses) {
            String expenseId = expense.getId();
            String expenseTitle = expense.getTitle();
            String payerUid = expense.getPayerUid();
            String payerName = expense.getPayerName();
            double totalAmount = expense.getAmount();
            long timestamp = expense.getTimestamp();
            Map<String, Double> splitAmounts = expense.getSplitAmounts();
            
            if (splitAmounts == null || splitAmounts.isEmpty()) {
                // Skip expenses without breakdown map
                continue;
            }
            
            // Create Settlement Objects: For every entry in the breakdown map
            for (Map.Entry<String, Double> entry : splitAmounts.entrySet()) {
                String debtorUid = entry.getKey();
                Double amountOwed = entry.getValue();
                
                // Skip if amount is zero or negative, or if debtor is the payer (self-payment)
                if (amountOwed == null || amountOwed <= 0 || debtorUid.equals(payerUid)) {
                    continue;
                }
                
                // Case C (Irrelevant): If current user is neither payer nor debtor, skip this item
                if (!currentUserId.equals(payerUid) && !currentUserId.equals(debtorUid)) {
                    continue;
                }
                
                // Check if this specific debt is settled and get settled date
                boolean isSettled = expense.isSettledFor(debtorUid);
                String settledDate = null;
                if (isSettled) {
                    Long settledTimestamp = expense.getSettledDateFor(debtorUid);
                    if (settledTimestamp != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        settledDate = dateFormat.format(new Date(settledTimestamp));
                    } else {
                        settledDate = "Settled";
                    }
                }
                
                // Determine perspective and description text
                String perspectiveText;
                if (currentUserId.equals(debtorUid)) {
                    // Case A (I owe someone): "You owe [PayerName] PHP XXX"
                    perspectiveText = "You owe " + payerName;
                } else if (currentUserId.equals(payerUid)) {
                    // Case B (Someone owes me): "[DebtorName] owes you PHP XXX"
                    String debtorName = memberNames.getOrDefault(debtorUid, shortenUid(debtorUid));
                    perspectiveText = debtorName + " owes you";
                } else {
                    continue; // Should not reach here due to Case C check
                }
                
                // Create individual settlement object with perspective
                SettlementDetail settlement = new SettlementDetail(
                        expenseId,                    // The original expense ID
                        expenseTitle,                 // The expense name
                        payerUid,                     // The payer's UID
                        payerName,                    // The payer's name
                        totalAmount,                  // Total expense amount
                        debtorUid,                    // The debtor's UID (person who owes)
                        payerUid,                     // The creditor's UID (person who is owed - the payer)
                        amountOwed,                   // Amount owed for this specific item
                        isSettled,                    // Whether this debt has been marked as settled
                        timestamp,                    // Date added (expense timestamp)
                        settledDate,                   // Settled date (null if not settled)
                        perspectiveText              // Perspective text for proper display
                );
                
                settlements.add(settlement);
            }
        }
        
        // Data Sorting: Sort the list by Date Added (descending) so newest expenses appear at top
        settlements.sort((a, b) -> Long.compare(b.dateAdded, a.dateAdded));
        
        return settlements;
    }
    
    private static String shortenUid(String uid) {
        return uid != null && uid.length() > 6 ? uid.substring(0, 6) : (uid != null ? uid : "?");
    }

    /**
     * Calculates net balance for the current user across all settlement details.
     * Positive value = user is owed money
     * Negative value = user owes money
     * 
     * @param currentUserId    The current user's UID
     * @param settlements      List of settlement details
     * @return Net balance
     */
    public static double calculateNetBalance(String currentUserId, List<SettlementDetail> settlements) {
        double netBalance = 0;
        
        for (SettlementDetail settlement : settlements) {
            if (settlement.creditorUid.equals(currentUserId)) {
                // User is the creditor (owed money)
                netBalance += settlement.settlementAmount;
            } else if (settlement.debtorUid.equals(currentUserId)) {
                // User is the debtor (owes money)
                netBalance -= settlement.settlementAmount;
            }
        }
        
        return Math.round(netBalance * 100.0) / 100.0;
    }
}
