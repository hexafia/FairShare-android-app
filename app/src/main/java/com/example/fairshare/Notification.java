package com.example.fairshare;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Represents a notification in the Firestore notifications collection
 * Used for both IOU Pinger notifications and payment confirmations
 */
public class Notification {

    @DocumentId
    private String id;
    private String type; // "nudge" or "payment_confirmed"
    private String recipientUid; // User who should receive this notification
    private String senderUid; // User who sent this notification
    private String senderName; // Display name of sender
    private String groupId; // Related group ID
    private String groupName; // Related group name for display
    private String expenseId; // Related expense ID
    private String expenseName; // Related expense name
    private double amount; // Amount involved
    private String message; // Formatted notification message
    private boolean isRead; // Whether notification has been read
    @ServerTimestamp
    private Date timestamp;

    // Required empty constructor for Firebase deserialization
    public Notification() {
    }

    public Notification(String type, String recipientUid, String senderUid, String senderName,
                       String groupId, String groupName, String expenseId, String expenseName,
                       double amount, String message) {
        this.type = type;
        this.recipientUid = recipientUid;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.groupId = groupId;
        this.groupName = groupName;
        this.expenseId = expenseId;
        this.expenseName = expenseName;
        this.amount = amount;
        this.message = message;
        this.isRead = false;
        this.timestamp = new Date();
    }

    // Static factory methods for creating different notification types
    public static Notification createNudgeNotification(String recipientUid, String senderUid, String senderName,
                                                     String groupId, String groupName, String expenseId, 
                                                     String expenseName, double amount) {
        String message = senderName + " nudged you. You still have a balance of Php" + String.format("%.2f", amount) + 
                        " for " + expenseName + " in " + groupName;
        return new Notification("nudge", recipientUid, senderUid, senderName, groupId, groupName,
                              expenseId, expenseName, amount, message);
    }

    public static Notification createPaymentConfirmationNotification(String recipientUid, String senderUid, String senderName,
                                                                   String groupId, String groupName, String expenseId, 
                                                                   String expenseName, double amount) {
        String message = senderName + " has confirmed your payment for " + expenseName + 
                        " in " + groupName + " worth Php" + String.format("%.2f", amount);
        return new Notification("payment_confirmed", recipientUid, senderUid, senderName, groupId, groupName,
                              expenseId, expenseName, amount, message);
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRecipientUid() { return recipientUid; }
    public void setRecipientUid(String recipientUid) { this.recipientUid = recipientUid; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }

    public String getExpenseName() { return expenseName; }
    public void setExpenseName(String expenseName) { this.expenseName = expenseName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
