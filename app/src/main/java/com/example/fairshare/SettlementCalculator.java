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
     * Calculates aggregated settlement details for all expenses in the group.
     * This method implements global tally logic to aggregate all outstanding debts.
     * 
     * @param currentUserId  The current user's UID (for filtering if needed)
     * @param expenses       List of all group expenses
     * @param memberNames    Map of uid -> display name
     * @return List of aggregated settlement details
     */
    public static List<SettlementDetail> calculateSettlements(
            String currentUserId, 
            List<GroupExpense> expenses,
            Map<String, String> memberNames) {
        
        List<SettlementDetail> settlements = new ArrayList<>();
        
        if (expenses == null || expenses.isEmpty()) {
            return settlements;
        }
        
        // Step 1: Global Tally Logic
        Map<String, Double> netBalances = new HashMap<>();
        
        for (GroupExpense expense : expenses) {
            String payerUid = expense.getPayerUid();
            double totalAmount = expense.getAmount();
            Map<String, Double> splitAmounts = expense.getSplitAmounts();
            
            if (splitAmounts == null || splitAmounts.isEmpty()) {
                // Skip expenses without breakdown map
                continue;
            }
            
            // Credit the Payer: Add the totalAmount to the payer's UID in the map
            netBalances.put(payerUid, netBalances.getOrDefault(payerUid, 0.0) + totalAmount);
            
            // Debit the Participants: Subtract each participant's share from their UID
            for (Map.Entry<String, Double> entry : splitAmounts.entrySet()) {
                String participantUid = entry.getKey();
                Double shareAmount = entry.getValue();
                
                if (shareAmount != null && shareAmount > 0) {
                    netBalances.put(participantUid, netBalances.getOrDefault(participantUid, 0.0) - shareAmount);
                }
            }
        }
        
        // Step 2: The 'Who Owes Whom' Algorithm
        // Separate debtors (negative balance) and creditors (positive balance)
        List<Map.Entry<String, Double>> debtors = new ArrayList<>();
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
            double balance = Math.round(entry.getValue() * 100.0) / 100.0; // Round to 2 decimal places
            
            if (balance < -0.01) { // Negative balance (owes money)
                debtors.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), -balance)); // Store as positive amount owed
            } else if (balance > 0.01) { // Positive balance (is owed money)
                creditors.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), balance));
            }
        }
        
        // Step 3: Match debtors to creditors to create settlement transactions
        int debtorIndex = 0;
        int creditorIndex = 0;
        
        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            Map.Entry<String, Double> debtor = debtors.get(debtorIndex);
            Map.Entry<String, Double> creditor = creditors.get(creditorIndex);
            
            String debtorUid = debtor.getKey();
            double debtorOwes = debtor.getValue();
            String creditorUid = creditor.getKey();
            double creditorIsOwed = creditor.getValue();
            
            // Calculate the settlement amount (minimum of what debtor owes and what creditor is owed)
            double settlementAmount = Math.min(debtorOwes, creditorIsOwed);
            settlementAmount = Math.round(settlementAmount * 100.0) / 100.0; // Round to 2 decimal places
            
            if (settlementAmount > 0.01) { // Only create settlement if amount is significant
                String debtorName = memberNames.getOrDefault(debtorUid, debtorUid);
                String creditorName = memberNames.getOrDefault(creditorUid, creditorUid);
                
                // Create aggregated settlement detail
                settlements.add(new SettlementDetail(
                        "aggregated", // Special ID for aggregated settlements
                        "Multiple Expenses", // Generic title for aggregated settlements
                        creditorUid, // The person who is owed
                        creditorName,
                        settlementAmount,
                        debtorUid, // The person who owes
                        creditorUid, // To the creditor
                        settlementAmount,
                        false // Aggregated settlements are not marked as settled individually
                ));
            }
            
            // Update remaining amounts
            double remainingDebtorOwes = debtorOwes - settlementAmount;
            double remainingCreditorIsOwed = creditorIsOwed - settlementAmount;
            
            // Update debtor
            if (remainingDebtorOwes > 0.01) {
                debtors.set(debtorIndex, new java.util.AbstractMap.SimpleEntry<>(debtorUid, remainingDebtorOwes));
            } else {
                debtorIndex++;
            }
            
            // Update creditor
            if (remainingCreditorIsOwed > 0.01) {
                creditors.set(creditorIndex, new java.util.AbstractMap.SimpleEntry<>(creditorUid, remainingCreditorIsOwed));
            } else {
                creditorIndex++;
            }
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
