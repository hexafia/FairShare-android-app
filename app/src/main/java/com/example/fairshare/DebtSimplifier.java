package com.example.fairshare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the "Minimum Debt Path" algorithm.
 *
 * Given a list of group expenses with equal splits, this class calculates:
 * 1. The net balance for each member (Total Paid - Total Owed).
 * 2. The minimum set of transactions needed to settle all debts.
 *
 * For example, if A paid ₱300 for A, B, C:
 *   - A's net = +200 (paid 300, owes 100)
 *   - B's net = -100 (paid 0, owes 100)
 *   - C's net = -100 (paid 0, owes 100)
 *   Result: B → A ₱100, C → A ₱100
 */
public class DebtSimplifier {

    /**
     * Represents a single settlement transaction: debtor pays creditor.
     */
    public static class Debt {
        public final String debtorUid;
        public final String creditorUid;
        public final double amount;

        public Debt(String debtorUid, String creditorUid, double amount) {
            this.debtorUid = debtorUid;
            this.creditorUid = creditorUid;
            this.amount = amount;
        }
    }

    /**
     * Calculates the minimum set of debts to settle all expenses.
     *
     * Supports multiple split types:
     * - If splitAmounts is available, uses explicit amounts per participant
     * - Otherwise, assumes equal split (amount / participantCount)
     *
     * @param expenses List of group expenses
     * @return List of Debt objects representing who owes whom and how much
     */
    public static List<Debt> simplify(List<GroupExpense> expenses) {
        // Step 1: Calculate net balance for each member
        Map<String, Double> netBalances = new HashMap<>();

        for (GroupExpense expense : expenses) {
            String payerUid = expense.getPayerUid();
            double amount = expense.getAmount();
            List<String> participants = expense.getParticipants();
            Map<String, Double> splitAmounts = expense.getSplitAmounts();

            if (participants == null || participants.isEmpty()) continue;

            // The payer paid the full amount
            netBalances.put(payerUid,
                    netBalances.getOrDefault(payerUid, 0.0) + amount);

            // Determine shares: use explicit splitAmounts if available, otherwise equal split
            if (splitAmounts != null && !splitAmounts.isEmpty()) {
                // Use explicit split amounts
                for (Map.Entry<String, Double> split : splitAmounts.entrySet()) {
                    String uid = split.getKey();
                    double shareAmount = split.getValue();
                    netBalances.put(uid,
                            netBalances.getOrDefault(uid, 0.0) - shareAmount);
                }
            } else {
                // Fallback to equal split
                int participantCount = participants.size();
                double sharePerPerson = amount / participantCount;
                for (String uid : participants) {
                    netBalances.put(uid,
                            netBalances.getOrDefault(uid, 0.0) - sharePerPerson);
                }
            }
        }

        // Step 2: Separate into creditors (+) and debtors (-)
        List<String> creditorUids = new ArrayList<>();
        List<Double> creditorAmounts = new ArrayList<>();
        List<String> debtorUids = new ArrayList<>();
        List<Double> debtorAmounts = new ArrayList<>();

        for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
            double balance = entry.getValue();
            // Use a small epsilon to handle floating point rounding
            if (balance > 0.01) {
                creditorUids.add(entry.getKey());
                creditorAmounts.add(balance);
            } else if (balance < -0.01) {
                debtorUids.add(entry.getKey());
                debtorAmounts.add(-balance); // store as positive
            }
        }

        // Step 3: Greedily match debtors to creditors
        List<Debt> debts = new ArrayList<>();
        int i = 0; // creditor index
        int j = 0; // debtor index

        while (i < creditorUids.size() && j < debtorUids.size()) {
            double credit = creditorAmounts.get(i);
            double debt = debtorAmounts.get(j);
            double settled = Math.min(credit, debt);

            // Round to 2 decimal places
            settled = Math.round(settled * 100.0) / 100.0;

            if (settled > 0) {
                debts.add(new Debt(debtorUids.get(j), creditorUids.get(i), settled));
            }

            creditorAmounts.set(i, credit - settled);
            debtorAmounts.set(j, debt - settled);

            if (creditorAmounts.get(i) < 0.01) i++;
            if (debtorAmounts.get(j) < 0.01) j++;
        }

        return debts;
    }
}
