package com.frauddetect;

import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.SettingsDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.dao.UserDAO;
import com.frauddetect.db.DBConnection;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Alert;
import com.frauddetect.model.Customer;
import com.frauddetect.model.Transaction;
import com.frauddetect.model.User;
import com.frauddetect.util.PasswordUtil;

import java.util.List;
import java.util.Map;

/**
 * Main application entry point for AI Fraud Detection & Transaction Monitoring System.
 * Phase 4 Verification: Tests DBConnection singleton, DAO implementations (UserDAO,
 * TransactionDAO, AlertDAO, SettingsDAO), one-time user seeding via PasswordUtil,
 * and outputs the database users and settings map.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" AI-Powered Financial Fraud Detection System - Phase 4 Verification");
        System.out.println("=========================================================================\n");

        try {
            // 1. RUBRIC 5: Database Connectivity via Singleton DBConnection
            System.out.println("--- 1. Testing Database Connection ---");
            DBConnection dbConnection = DBConnection.getInstance();
            boolean isConnected = dbConnection.testConnection();
            System.out.println("DB Connection Status: " + (isConnected ? "CONNECTED (SUCCESS)" : "FAILED"));

            // Initialize DAOs
            UserDAO userDAO = new UserDAO(dbConnection);
            TransactionDAO transactionDAO = new TransactionDAO(dbConnection);
            AlertDAO alertDAO = new AlertDAO(dbConnection);
            SettingsDAO settingsDAO = new SettingsDAO(dbConnection);

            // 2. One-time User Seeding via PasswordUtil
            System.out.println("\n--- 2. User Seeding via PasswordUtil (One-Time Execution) ---");
            seedUsersIfEmpty(userDAO);

            // 3. RUBRIC 4 & 6: Fetch and display Users polymorphically via UserDAO
            System.out.println("\n--- 3. Users in Database (Polymorphic UserDAO.findAll) ---");
            List<User> users = userDAO.findAll();
            System.out.printf("Total Users Retrieved: %d%n", users.size());
            for (User u : users) {
                System.out.printf("  ID: %d | Username: %-12s | Name: %-18s | Role: %-8s | %s%n",
                        u.getUserId(), u.getUsername(), u.getFullName(), u.getRole(), u.getDashboardTitle());
            }

            // 4. RUBRIC 2 & 4: Fetch and display Settings Map via SettingsDAO
            System.out.println("\n--- 4. Settings Map in Database (SettingsDAO.getAll) ---");
            Map<String, String> settingsMap = settingsDAO.getAll();
            System.out.printf("Total Settings Configured: %d%n", settingsMap.size());
            settingsMap.forEach((key, val) -> System.out.printf("  %-28s = %s%n", key, val));

            // 5. Sample Transactions & Stats Verification via TransactionDAO
            System.out.println("\n--- 5. TransactionDAO & AlertDAO Database Verification ---");
            List<Transaction> transactions = transactionDAO.findAll();
            System.out.printf("Total Transactions in DB : %d%n", transactions.size());

            TransactionDAO.AmountStats statsUser2 = transactionDAO.getAmountStats(2);
            System.out.printf("User 2 (john_doe) Historical Stats: %s%n", statsUser2);

            List<Alert> alerts = alertDAO.findAll();
            System.out.printf("Total Alerts in DB       : %d%n", alerts.size());
            for (Alert a : alerts) {
                System.out.printf("  Alert #%d on Txn #%d | Risk: %-6s | Note: %s%n",
                        a.getAlertId(), a.getTxnId(), a.getRiskLevel(),
                        (a.getAdminNote() != null ? a.getAdminNote() : "Pending Admin Review"));
            }

            System.out.println("\n=========================================================================");
            System.out.println(" Phase 4 Verification Completed Successfully!");
            System.out.println("=========================================================================");

        } catch (DatabaseException e) {
            System.err.println("Database Exception occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanly shut down MySQL driver background thread to allow instant process exit
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }

    /**
     * Seeds initial user credentials into the database using PasswordUtil (SHA-256 + salt)
     * if the users table is currently empty.
     */
    public static void seedUsersIfEmpty(UserDAO userDAO) throws DatabaseException {
        List<User> existingUsers = userDAO.findAll();
        if (!existingUsers.isEmpty()) {
            System.out.println("Users already present in database (" + existingUsers.size() + " users found). Skipping seeding.");
            return;
        }

        System.out.println("Users table is empty. Seeding initial accounts using PasswordUtil with secure salts...");

        // 1. Admin Account (User ID 1)
        String adminSalt = PasswordUtil.generateSalt();
        String adminHash = PasswordUtil.hash("admin123", adminSalt);
        Admin admin = new Admin(0, "admin", adminHash, adminSalt, "Security Administrator", "admin@frauddetect.com", "India", null);
        userDAO.save(admin);
        System.out.println("  [+] Created Admin: admin (pwd: admin123)");

        // 2. Customer 1 (User ID 2)
        String c1Salt = PasswordUtil.generateSalt();
        String c1Hash = PasswordUtil.hash("password123", c1Salt);
        Customer c1 = new Customer(0, "john_doe", c1Hash, c1Salt, "John Doe", "john@example.com", "India", null);
        userDAO.save(c1);
        System.out.println("  [+] Created Customer: john_doe (pwd: password123)");

        // 3. Customer 2 (User ID 3)
        String c2Salt = PasswordUtil.generateSalt();
        String c2Hash = PasswordUtil.hash("password123", c2Salt);
        Customer c2 = new Customer(0, "priya_sharma", c2Hash, c2Salt, "Priya Sharma", "priya@example.com", "India", null);
        userDAO.save(c2);
        System.out.println("  [+] Created Customer: priya_sharma (pwd: password123)");

        // 4. Customer 3 (User ID 4)
        String c3Salt = PasswordUtil.generateSalt();
        String c3Hash = PasswordUtil.hash("password123", c3Salt);
        Customer c3 = new Customer(0, "rahul_verma", c3Hash, c3Salt, "Rahul Verma", "rahul@example.com", "India", null);
        userDAO.save(c3);
        System.out.println("  [+] Created Customer: rahul_verma (pwd: password123)");

        System.out.println("Seeding completed successfully!");
    }
}
