package com.frauddetect;

import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.SettingsDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.dao.UserDAO;
import com.frauddetect.db.DBConnection;
import com.frauddetect.detection.FraudDetectionEngine;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Customer;
import com.frauddetect.model.DetectionResult;
import com.frauddetect.model.DomesticTransaction;
import com.frauddetect.model.InternationalTransaction;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;
import com.frauddetect.model.User;
import com.frauddetect.util.PasswordUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Main application entry point for AI Fraud Detection & Transaction Monitoring System.
 * Phase 5 Verification: Tests the Hybrid Fraud Detection Engine (Rule-based + Statistical Anomaly).
 * Demonstrates LOW risk for a normal ₹2,000 domestic transaction and HIGH risk for a ₹95,000
 * international transaction executed at 03:00 AM.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" AI-Powered Financial Fraud Detection System - Phase 5 Verification");
        System.out.println("=========================================================================\n");

        try {
            DBConnection dbConnection = DBConnection.getInstance();
            UserDAO userDAO = new UserDAO(dbConnection);
            TransactionDAO transactionDAO = new TransactionDAO(dbConnection);
            AlertDAO alertDAO = new AlertDAO(dbConnection);
            SettingsDAO settingsDAO = new SettingsDAO(dbConnection);

            // Ensure database users are seeded
            seedUsersIfEmpty(userDAO);

            // Instantiate Detection Engine
            FraudDetectionEngine engine = new FraudDetectionEngine(settingsDAO, userDAO, transactionDAO);
            System.out.printf("Detection Engine Initialized with %d Active Rules.%n", engine.getRules().size());
            System.out.printf("Risk Cutoffs: MEDIUM >= %d | HIGH >= %d%n%n",
                    engine.getMediumRiskCutoff(), engine.getHighRiskCutoff());

            // -----------------------------------------------------------------
            // Test Scenario 1: Normal Everyday Domestic Transaction
            // -----------------------------------------------------------------
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("TEST 1: Normal Domestic Transaction (₹2,000 - Mumbai, Daytime)");
            System.out.println("-------------------------------------------------------------------------");
            LocalDateTime daytime = LocalDateTime.now().withHour(14).withMinute(30); // 2:30 PM
            Transaction normalTxn = new DomesticTransaction(
                    0, 2, 2000.00, "987654321001", "Mumbai", "India",
                    "Grocery store purchase", Timestamp.valueOf(daytime), 0, RiskLevel.LOW, "PENDING"
            );

            DetectionResult normalResult = engine.analyze(normalTxn);
            System.out.printf("Result Score  : %d / 100%n", normalResult.getScore());
            System.out.printf("Risk Level    : %s%n", normalResult.getLevel());
            System.out.printf("Decision      : %s%n", normalResult.getRecommendedStatus());
            System.out.printf("Reasons       : %s%n", normalResult.getFormattedReasons());
            System.out.printf("Status Check  : %s%n%n",
                    (normalResult.getLevel() == RiskLevel.LOW ? "PASSED (LOW RISK)" : "FAILED"));

            // -----------------------------------------------------------------
            // Test Scenario 2: High-Risk International Transaction at 03:00 AM
            // -----------------------------------------------------------------
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("TEST 2: Suspicious International Transaction (₹95,000 - Dubai, UAE at 03:00 AM)");
            System.out.println("-------------------------------------------------------------------------");
            LocalDateTime nightTime = LocalDateTime.now().withHour(3).withMinute(0); // 03:00 AM
            Transaction suspiciousTxn = new InternationalTransaction(
                    0, 2, 95000.00, "112233445566", "Dubai", "UAE",
                    "High-value overseas electronics import", Timestamp.valueOf(nightTime), 0, RiskLevel.LOW, "PENDING"
            );

            DetectionResult suspiciousResult = engine.analyze(suspiciousTxn);
            System.out.printf("Result Score  : %d / 100%n", suspiciousResult.getScore());
            System.out.printf("Risk Level    : %s%n", suspiciousResult.getLevel());
            System.out.printf("Decision      : %s%n", suspiciousResult.getRecommendedStatus());
            System.out.println("Triggered Explainable Reasons:");
            for (String r : suspiciousResult.getReasons()) {
                System.out.printf("  [!] %s%n", r);
            }
            System.out.printf("Status Check  : %s%n%n",
                    (suspiciousResult.getLevel() == RiskLevel.HIGH ? "PASSED (HIGH RISK - BLOCKED)" : "FAILED"));

            // -----------------------------------------------------------------
            // Test Scenario 3: Statistical Anomaly (Z-Score) Demonstration
            // -----------------------------------------------------------------
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("TEST 3: Statistical Anomaly Check (Z-Score on User 3 Priya Sharma)");
            System.out.println("-------------------------------------------------------------------------");
            Transaction anomalyTxn = new DomesticTransaction(
                    0, 3, 185000.00, "887766554415", "Bengaluru", "India",
                    "Unusual high-stake investment transfer", Timestamp.valueOf(daytime), 0, RiskLevel.LOW, "PENDING"
            );
            DetectionResult anomalyResult = engine.analyze(anomalyTxn);
            System.out.printf("Result Score  : %d / 100%n", anomalyResult.getScore());
            System.out.printf("Risk Level    : %s%n", anomalyResult.getLevel());
            System.out.printf("Decision      : %s%n", anomalyResult.getRecommendedStatus());
            System.out.println("Triggered Reasons:");
            for (String r : anomalyResult.getReasons()) {
                System.out.printf("  [!] %s%n", r);
            }

            System.out.println("\n=========================================================================");
            System.out.println(" Phase 5 Verification Completed Successfully!");
            System.out.println("=========================================================================");

        } catch (DatabaseException e) {
            System.err.println("Database Exception occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }

    public static void seedUsersIfEmpty(UserDAO userDAO) throws DatabaseException {
        List<User> existingUsers = userDAO.findAll();
        if (!existingUsers.isEmpty()) {
            return;
        }

        String adminSalt = PasswordUtil.generateSalt();
        String adminHash = PasswordUtil.hash("admin123", adminSalt);
        userDAO.save(new Admin(0, "admin", adminHash, adminSalt, "Security Administrator", "admin@frauddetect.com", "India", null));

        String c1Salt = PasswordUtil.generateSalt();
        String c1Hash = PasswordUtil.hash("password123", c1Salt);
        userDAO.save(new Customer(0, "john_doe", c1Hash, c1Salt, "John Doe", "john@example.com", "India", null));

        String c2Salt = PasswordUtil.generateSalt();
        String c2Hash = PasswordUtil.hash("password123", c2Salt);
        userDAO.save(new Customer(0, "priya_sharma", c2Hash, c2Salt, "Priya Sharma", "priya@example.com", "India", null));

        String c3Salt = PasswordUtil.generateSalt();
        String c3Hash = PasswordUtil.hash("password123", c3Salt);
        userDAO.save(new Customer(0, "rahul_verma", c3Hash, c3Salt, "Rahul Verma", "rahul@example.com", "India", null));
    }
}
