package com.example.fairshare;

import com.google.firebase.firestore.DocumentId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a shared expense within a group, stored in Firestore
 * at path: group_expenses/{expenseId}
 */
public class GroupExpense {

    @DocumentId
    private String id;
    private String groupId;
    private String title;
    private String payerUid;
    private String payerName;
    private double amount;
    private String splitType; // "EQUAL", "SELECTIVE", "PERCENTAGE", "AMOUNT"
    private String category; // "Food", "Transport", "Shopping", "Bills", "Other"
    private List<String> participants;
    private Map<String, Double> splitAmounts; // uid -> exact amount owed (breakdown map)
    private List<String> involvedUsers; // payerUid + all participants (for efficient querying)
    private long timestamp;
    private Map<String, Boolean> settledStatus; // uid -> true if that person's share is settled
    private Map<String, Long> settledDates; // uid -> timestamp when that person's share was settled

    // Required empty constructor for Firebase deserialization
    public GroupExpense() {
        participants = new ArrayList<>();
        settledStatus = new HashMap<>();
        settledDates = new HashMap<>();
    }

    public GroupExpense(String groupId, String title, String payerUid, String payerName, double amount) {
        this.groupId = groupId;
        this.title = title;
        this.payerUid = payerUid;
        this.payerName = payerName;
        this.amount = amount;
        this.splitType = "EQUAL";
        this.participants = new ArrayList<>();
        this.settledStatus = new HashMap<>();
        this.settledDates = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
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

    public Map<String, Double> getSplitAmounts() {
        return splitAmounts;
    }

    public void setSplitAmounts(Map<String, Double> splitAmounts) {
        this.splitAmounts = splitAmounts;
    }

    public List<String> getInvolvedUsers() {
        return involvedUsers;
    }

    public void setInvolvedUsers(List<String> involvedUsers) {
        this.involvedUsers = involvedUsers;
    }

    public Map<String, Boolean> getSettledStatus() {
        return settledStatus;
    }

    public void setSettledStatus(Map<String, Boolean> settledStatus) {
        this.settledStatus = settledStatus;
    }

    public Map<String, Long> getSettledDates() {
        return settledDates;
    }

    public void setSettledDates(Map<String, Long> settledDates) {
        this.settledDates = settledDates;
    }

    public boolean isSettledFor(String uid) {
        return settledStatus != null && Boolean.TRUE.equals(settledStatus.get(uid));
    }

    public Long getSettledDateFor(String uid) {
        return settledDates != null ? settledDates.get(uid) : null;
    }
}
