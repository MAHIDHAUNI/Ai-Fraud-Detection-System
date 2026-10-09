package com.frauddetect.model;

import java.sql.Timestamp;

/**
 * RUBRIC: 1 - OOP: Abstraction, Inheritance & Polymorphism
 * Abstract base class representing a financial transaction.
 * Declares abstract operations (getRiskMultiplier, getCategory) that specialized
 * transaction subclasses implement with specific risk characteristics.
 */
public abstract class Transaction {
    // Encapsulation: Protected/Private attributes matching the transactions table schema
    private int txnId;
    private int userId;
    private double amount;
    private String txnType;
    private String receiverAccount;
    private String location;
    private String country;
    private String description;
    private Timestamp txnTime;
    private int riskScore;
    private RiskLevel riskLevel;
    private String status; // 'PENDING', 'APPROVED', 'FLAGGED', 'BLOCKED'

    public Transaction() {
        this.status = "PENDING";
        this.riskLevel = RiskLevel.LOW;
        this.riskScore = 0;
        this.txnTime = new Timestamp(System.currentTimeMillis());
    }

    public Transaction(int userId, double amount, String txnType, String receiverAccount,
                       String location, String country, String description) {
        this.userId = userId;
        this.amount = amount;
        this.txnType = txnType;
        this.receiverAccount = receiverAccount;
        this.location = location;
        this.country = country;
        this.description = description;
        this.status = "PENDING";
        this.riskLevel = RiskLevel.LOW;
        this.riskScore = 0;
        this.txnTime = new Timestamp(System.currentTimeMillis());
    }

    public Transaction(int txnId, int userId, double amount, String txnType,
                       String receiverAccount, String location, String country, String description) {
        this.txnId = txnId;
        this.userId = userId;
        this.amount = amount;
        this.txnType = txnType;
        this.receiverAccount = receiverAccount;
        this.location = location;
        this.country = country;
        this.description = description;
        this.status = "PENDING";
        this.riskLevel = RiskLevel.LOW;
        this.riskScore = 0;
        this.txnTime = new Timestamp(System.currentTimeMillis());
    }

    public Transaction(int txnId, int userId, double amount, String txnType,
                       String receiverAccount, String location, String country,
                       String description, Timestamp txnTime, int riskScore,
                       RiskLevel riskLevel, String status) {
        this.txnId = txnId;
        this.userId = userId;
        this.amount = amount;
        this.txnType = txnType;
        this.receiverAccount = receiverAccount;
        this.location = location;
        this.country = country;
        this.description = description;
        this.txnTime = txnTime;
        this.riskScore = riskScore;
        this.riskLevel = (riskLevel != null) ? riskLevel : RiskLevel.LOW;
        this.status = (status != null && !status.isBlank()) ? status : "PENDING";
    }

    // Polymorphism: Subclasses supply their specific risk weighting and display category
    public abstract double getRiskMultiplier();
    public abstract String getCategory();

    // Getters and Setters
    public int getTxnId() {
        return txnId;
    }

    public void setTxnId(int txnId) {
        this.txnId = txnId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(String receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getTxnTime() {
        return txnTime;
    }

    public void setTxnTime(Timestamp txnTime) {
        this.txnTime = txnTime;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("[%s #%d] Amount: ₹%.2f, Receiver: %s, Score: %d (%s), Status: %s, Category: %s",
                getTxnType(), txnId, amount, receiverAccount, riskScore, riskLevel, status, getCategory());
    }
}
