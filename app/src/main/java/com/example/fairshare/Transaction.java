package com.example.fairshare;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Represents a single personal expense transaction.
 * Fields map directly to a Firestore document in the "expenses" collection.
 * NOTE: All transactions are now treated as expenses (income tracking removed).
 */
public class Transaction {

    @DocumentId
    private String id;
    private String title;
    private double amount;
    private String category;
    private String uid;  // ID of the user who owns this transaction

    @ServerTimestamp
    private Date date;

    // Required empty constructor for Firestore deserialization
    public Transaction() {
    }

    public Transaction(String title, double amount, String category) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        // Provide a local timestamp so it instantly appears in the UI
        // Firestore's @ServerTimestamp will overwrite this with the true server time upon sync.
        this.date = new Date();
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
