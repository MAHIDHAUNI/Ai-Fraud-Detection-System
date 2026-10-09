package com.frauddetect.model;

import java.sql.Timestamp;

/**
 * RUBRIC: 1 - OOP: Inheritance & Polymorphism
 * Represents a cross-border international transaction.
 * International transactions carry higher inherent exposure, applying a 1.2x risk multiplier.
 */
public class InternationalTransaction extends Transaction {

    public InternationalTransaction() {
        super();
        setTxnType("INTERNATIONAL");
    }

    public InternationalTransaction(int userId, double amount, String receiverAccount,
                                     String location, String country, String description) {
        super(userId, amount, "INTERNATIONAL", receiverAccount, location, country, description);
    }

    public InternationalTransaction(int txnId, int userId, double amount, String receiverAccount,
                                     String location, String country, String description) {
        super(txnId, userId, amount, "INTERNATIONAL", receiverAccount, location, country, description);
    }

    public InternationalTransaction(int txnId, int userId, double amount, String receiverAccount,
                                     String location, String country, String description,
                                     Timestamp txnTime, int riskScore, RiskLevel riskLevel, String status) {
        super(txnId, userId, amount, "INTERNATIONAL", receiverAccount, location, country,
                description, txnTime, riskScore, riskLevel, status);
    }

    // Polymorphism: Cross-border transfers elevate risk by 20% (1.2x multiplier)
    @Override
    public double getRiskMultiplier() {
        return 1.2;
    }

    // Polymorphism: Category descriptor for reports and GUI
    @Override
    public String getCategory() {
        return "International Cross-Border Transfer";
    }
}
