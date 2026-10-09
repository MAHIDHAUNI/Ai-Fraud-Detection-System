package com.frauddetect.model;

import java.sql.Timestamp;

/**
 * RUBRIC: 1 - OOP: Encapsulation & Domain Modeling
 * Represents a security alert raised by the detection engine when a transaction
 * exhibits MEDIUM or HIGH risk characteristics.
 */
public class Alert {
    private int alertId;
    private int txnId;
    private int userId;
    private RiskLevel riskLevel;
    private String reasons;
    private Timestamp alertTime;
    private boolean resolved;
    private String adminNote;

    public Alert() {
        this.resolved = false;
        this.alertTime = new Timestamp(System.currentTimeMillis());
    }

    public Alert(int txnId, int userId, RiskLevel riskLevel, String reasons) {
        this.txnId = txnId;
        this.userId = userId;
        this.riskLevel = riskLevel;
        this.reasons = reasons;
        this.resolved = false;
        this.alertTime = new Timestamp(System.currentTimeMillis());
    }

    public Alert(int alertId, int txnId, int userId, RiskLevel riskLevel,
                 String reasons, Timestamp alertTime, boolean resolved, String adminNote) {
        this.alertId = alertId;
        this.txnId = txnId;
        this.userId = userId;
        this.riskLevel = riskLevel;
        this.reasons = reasons;
        this.alertTime = alertTime;
        this.resolved = resolved;
        this.adminNote = adminNote;
    }

    // Getters and Setters
    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

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

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }

    public Timestamp getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(Timestamp alertTime) {
        this.alertTime = alertTime;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    @Override
    public String toString() {
        return String.format("Alert #%d [Txn #%d, User #%d, Level: %s, Resolved: %b, Note: %s]",
                alertId, txnId, userId, riskLevel, resolved, (adminNote != null ? adminNote : "N/A"));
    }
}
