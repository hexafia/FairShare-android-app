package com.example.fairshare;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a shared expense within a group, stored in Firebase Realtime Database
 * at path: group_expenses/{groupId}/{expenseId}
 */
public class GroupExpense {

    private String id;
    private String title;
    private String payerUid;
    private String payerName;
    private double amount;
    private String splitType; // "EQUITY" for the current contribution ledger
    private Map<String, Boolean> participants;
    private Map<String, Double> contributionAmounts;
    private long timestamp;

    // Required empty constructor for Firebase deserialization
    public GroupExpense() {
        participants = new HashMap<>();
        contributionAmounts = new HashMap<>();
    }

    public GroupExpense(String title, String payerUid, String payerName, double amount) {
        this.title = title;
        this.payerUid = payerUid;
        this.payerName = payerName;
        this.amount = amount;
        this.splitType = "EQUITY";
        this.participants = new HashMap<>();
        this.contributionAmounts = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPayerUid() {
        return payerUid;
    }

    public void setPayerUid(String payerUid) {
        this.payerUid = payerUid;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public Map<String, Boolean> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<String, Boolean> participants) {
        this.participants = participants;
    }

    public Map<String, Double> getContributionAmounts() {
        if (contributionAmounts == null) {
            contributionAmounts = new HashMap<>();
        }
        return contributionAmounts;
    }

    public void setContributionAmounts(Map<String, Double> contributionAmounts) {
        this.contributionAmounts = contributionAmounts;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getParticipantCount() {
        return participants != null ? participants.size() : 0;
    }

    public double getContributionTotal() {
        if (contributionAmounts != null && !contributionAmounts.isEmpty()) {
            double total = 0d;
            for (Double value : contributionAmounts.values()) {
                if (value != null) {
                    total += value;
                }
            }
            return total;
        }
        return amount;
    }

    public int getContributorCount() {
        return contributionAmounts != null ? contributionAmounts.size() : 0;
    }
}
