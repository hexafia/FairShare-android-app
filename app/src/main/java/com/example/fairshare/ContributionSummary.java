package com.example.fairshare;

public class ContributionSummary {

    private final String uid;
    private final String name;
    private final double amount;
    private final double percentage;

    public ContributionSummary(String uid, String name, double amount, double percentage) {
        this.uid = uid;
        this.name = name;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public double getPercentage() {
        return percentage;
    }
}
