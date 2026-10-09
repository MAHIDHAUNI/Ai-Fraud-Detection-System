package com.frauddetect.model;

import java.sql.Timestamp;

/**
 * RUBRIC: 1 - OOP: Inheritance & Polymorphism
 * Represents a domestic transaction executed within the user's home nation.
 * Standard domestic transactions have a baseline risk multiplier of 1.0.
 */
public class DomesticTransaction extends Transaction {

    public DomesticTransaction() {
        super();
        setTxnType("DOMESTIC");
    }

    public DomesticTransaction(int userId, double amount, String receiverAccount,
                               String location, String country, String description) {
        super(userId, amount, "DOMESTIC", receiverAccount, location, country, description);
    }

    public DomesticTransaction(int txnId, int userId, double amount, String receiverAccount,
                               String location, String country, String description) {
        super(txnId, userId, amount, "DOMESTIC", receiverAccount, location, country, description);
    }

    public DomesticTransaction(int txnId, int userId, double amount, String receiverAccount,
                               String location, String country, String description,
                               Timestamp txnTime, int riskScore, RiskLevel riskLevel, String status) {
        super(txnId, userId, amount, "DOMESTIC", receiverAccount, location, country,
                description, txnTime, riskScore, riskLevel, status);
    }

    // Polymorphism: Domestic transfers apply standard base risk factor (1.0x)
    @Override
    public double getRiskMultiplier() {
        return 1.0;
    }

    // Polymorphism: Category descriptor for reports and GUI
    @Override
    public String getCategory() {
        return "Domestic Transfer";
    }
}
