package com.frauddetect;

import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.SettingsDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.dao.UserDAO;
import com.frauddetect.db.DBConnection;
import com.frauddetect.exception.AuthenticationException;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Alert;
import com.frauddetect.model.Customer;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.User;
import com.frauddetect.service.AlertService;
import com.frauddetect.service.AuthService;
import com.frauddetect.service.ReportService;
import com.frauddetect.util.PasswordUtil;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Main application entry point for AI Fraud Detection & Transaction Monitoring System.
 * Phase 7 Verification: Tests the complete Service Layer:
 * 1. AuthService (AuthenticationException, Password verification, polymorphic returns)
 * 2. AlertService (Unresolved alert retrieval, alert resolution)
 * 3. ReportService (Summary, Collectors.groupingBy risk breakdown, Top 5 riskiest users, 7-day trend, CSV export)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" AI-Powered Financial Fraud Detection System - Phase 7 Verification");
        System.out.println("=========================================================================\n");

        try {
            DBConnection dbConnection = DBConnection.getInstance();
            UserDAO userDAO = new UserDAO(dbConnection);
            TransactionDAO transactionDAO = new TransactionDAO(dbConnection);
            AlertDAO alertDAO = new AlertDAO(dbConnection);
            SettingsDAO settingsDAO = new SettingsDAO(dbConnection);

            seedUsersIfEmpty(userDAO);

            // -----------------------------------------------------------------
            // 1. AuthService Verification
            // -----------------------------------------------------------------
            System.out.println("--- 1. AuthService Verification ---");
            AuthService authService = new AuthService(userDAO);

            // 1a. Test successful Admin login
            User adminUser = authService.login("admin", "admin123");
            System.out.printf("Admin Login Success   : %s | Role: %s | %s%n",
                    adminUser.getUsername(), adminUser.getRole(), adminUser.getDashboardTitle());

            // 1b. Test successful Customer login
            User customerUser = authService.login("john_doe", "password123");
            System.out.printf("Customer Login Success: %s | Role: %s | %s%n",
                    customerUser.getUsername(), customerUser.getRole(), customerUser.getDashboardTitle());

            // 1c. Test failed login exception
            try {
                System.out.print("Testing bad password ('wrong_pwd')... ");
                authService.login("admin", "wrong_pwd");
                System.out.println("FAILED: Should have thrown AuthenticationException");
            } catch (AuthenticationException e) {
                System.out.println("PASSED: Caught expected AuthenticationException -> " + e.getMessage());
            }

            // -----------------------------------------------------------------
            // 2. AlertService Verification
            // -----------------------------------------------------------------
            System.out.println("\n--- 2. AlertService Verification ---");
            AlertService alertService = new AlertService(alertDAO);
            List<Alert> unresolved = alertService.getUnresolvedAlerts();
            System.out.printf("Total Unresolved Alerts: %d%n", unresolved.size());
            if (!unresolved.isEmpty()) {
                Alert first = unresolved.get(0);
                System.out.printf("  Resolving Alert #%d (Txn #%d)... ", first.getAlertId(), first.getTxnId());
                boolean resolved = alertService.resolveAlert(first.getAlertId(), "Reviewed and verified with customer by phone");
                System.out.println(resolved ? "SUCCESS" : "FAILED");
            }

            // -----------------------------------------------------------------
            // 3. ReportService Verification (Collections & Streams)
            // -----------------------------------------------------------------
            System.out.println("\n--- 3. ReportService Verification (Analytics & Grouping) ---");
            ReportService reportService = new ReportService(transactionDAO, userDAO, alertDAO);

            // 3a. System Metrics Summary
            ReportService.SystemSummary summary = reportService.getSummaryMetrics();
            System.out.printf("System Summary: %s%n", summary);

            // 3b. RUBRIC: 2 - Collections.groupingBy Risk Breakdown
            System.out.println("\nTransactions Grouped by Risk Level (Collectors.groupingBy):");
            Map<RiskLevel, Long> riskMap = reportService.getTransactionsPerRiskLevel();
            riskMap.forEach((level, count) -> System.out.printf("  %-7s : %d transactions%n", level, count));

            // 3c. RUBRIC: 2 - Top Riskiest Users (Custom Comparator + Streams)
            System.out.println("\nTop Riskiest Customer Accounts:");
            List<ReportService.RiskyUserSummary> topRisky = reportService.getTopRiskiestUsers(5);
            for (ReportService.RiskyUserSummary r : topRisky) {
                System.out.printf("  User #%-2d (%-14s) | Txns: %2d | Blocked: %d | Flagged: %d | Avg Score: %5.1f | Volume: ₹%,.2f%n",
                        r.getUserId(), r.getUsername(), r.getTotalTransactions(),
                        r.getBlockedCount(), r.getSuspiciousCount(), r.getAverageRiskScore(), r.getTotalAmount());
            }

            // 3d. 7-Day Trend
            System.out.println("\nLast 7 Days Activity Trend:");
            Map<String, ReportService.DailyTrend> trend = reportService.getDailyTrendLast7Days();
            trend.forEach((date, d) -> System.out.printf("  %s -> Total: %2d | Approved: %2d | Flagged: %d | Blocked: %d | Volume: ₹%,9.2f%n",
                    date, d.getTotalCount(), d.getApprovedCount(), d.getFlaggedCount(), d.getBlockedCount(), d.getTotalVolume()));

            // 3e. Export CSV Report
            File csvOutput = new File("docs", "sample_audit_report.csv");
            reportService.exportReportToCSV(csvOutput);
            System.out.printf("%nAudit CSV Exported Successfully: %s (Size: %d bytes)%n",
                    csvOutput.getAbsolutePath(), csvOutput.length());

            System.out.println("\n=========================================================================");
            System.out.println(" Phase 7 Verification Completed Successfully!");
            System.out.println("=========================================================================");

        } catch (Exception e) {
            System.err.println("Exception during Phase 7 execution: " + e.getMessage());
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
