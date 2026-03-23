package com.example.fairshare;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Represents a single expense transaction.
 * Fields map directly to a Firestore document in the "expenses" collection.
 */
public class Transaction {

    @DocumentId
    private String id;
    private String title;
    private double amount;
    private String category;
    private String type; // "expense" or "income"

    @ServerTimestamp
    private Date date;

    // Required empty constructor for Firestore deserialization
    public Transaction() {
    }

    public Transaction(String title, double amount, String category, String type) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
