package com.example.fairshare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        public SettlementDetail(String expenseId, String expenseTitle, String payerUid, 
                               String payerName, double amount, String debtorUid, 
                               String creditorUid, double settlementAmount, boolean settled) {
            this.expenseId = expenseId;
            this.expenseTitle = expenseTitle;
            this.payerUid = payerUid;
            this.payerName = payerName;
            this.amount = amount;
            this.debtorUid = debtorUid;
            this.creditorUid = creditorUid;
            this.settlementAmount = settlementAmount;
            this.settled = settled;
        }
    }

    /**
     * Calculates settlement details for the current user from all expenses they're involved in.
     * 
     * @param currentUserId  The current user's UID
     * @param expenses       List of expenses where the user is involved
     * @param memberNames    Map of uid -> display name
     * @return List of settlement details
     */
    public static List<SettlementDetail> calculateSettlements(
            String currentUserId, 
            List<GroupExpense> expenses,
            Map<String, String> memberNames) {
        
        List<SettlementDetail> settlements = new ArrayList<>();
        
        if (expenses == null || expenses.isEmpty()) {
            return settlements;
        }
        
        for (GroupExpense expense : expenses) {
            if (expense.getSplitAmounts() == null || expense.getSplitAmounts().isEmpty()) {
                // Skip expenses without breakdown map
                continue;
            }
            
            String payerUid = expense.getPayerUid();
            String payerName = expense.getPayerName();
            String expenseId = expense.getId();
            String expenseTitle = expense.getTitle();
            double expenseAmount = expense.getAmount();
            Map<String, Double> breakdown = expense.getSplitAmounts();
            
            if (payerUid.equals(currentUserId)) {
                // Case 1: Current user is the payer
                // They are owed the sum of all breakdown values excluding their own entry
                double totalOwed = 0;
                for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
                    String uid = entry.getKey();
                    Double amount = entry.getValue();
                    if (!uid.equals(currentUserId) && amount != null && amount > 0) {
                        totalOwed += amount;
                    }
                }
                
                // Create a settlement detail for each person who owes the payer
                for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
                    String debtorUid = entry.getKey();
                    Double owed = entry.getValue();
                    if (!debtorUid.equals(payerUid) && owed != null && owed > 0) {
                        String debtorName = memberNames.getOrDefault(debtorUid, debtorUid);
                        boolean isSettled = expense.isSettledFor(debtorUid);
                        settlements.add(new SettlementDetail(
                                expenseId,
                                expenseTitle,
                                payerUid,
                                payerName,
                                expenseAmount,
                                debtorUid,      // This person owes
                                payerUid,       // To the payer
                                owed,           // This specific amount
                                isSettled       // Settlement status
                        ));
                    }
                }
                
            } else if (breakdown.containsKey(currentUserId)) {
                // Case 2: Current user is not the payer but is in the breakdown
                // They owe the specific amount in breakdown to the payer
                Double owed = breakdown.get(currentUserId);
                if (owed != null && owed > 0) {
                    boolean isSettled = expense.isSettledFor(currentUserId);
                    settlements.add(new SettlementDetail(
                            expenseId,
                            expenseTitle,
                            payerUid,
                            payerName,
                            expenseAmount,
                            currentUserId,  // Current user owes
                            payerUid,       // To the payer
                            owed,           // This specific amount
                            isSettled       // Settlement status
                    ));
                }
            }
            // If current user is not involved in this expense, skip it
        }
        
        return settlements;
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
