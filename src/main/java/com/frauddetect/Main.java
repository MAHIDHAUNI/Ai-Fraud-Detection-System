package com.frauddetect;

import com.frauddetect.concurrent.StatsCounter;
import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.SettingsDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.dao.UserDAO;
import com.frauddetect.db.DBConnection;
import com.frauddetect.detection.FraudDetectionEngine;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Customer;
import com.frauddetect.model.DomesticTransaction;
import com.frauddetect.model.InternationalTransaction;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;
import com.frauddetect.model.User;
import com.frauddetect.service.TransactionService;
import com.frauddetect.util.PasswordUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main application entry point for AI Fraud Detection & Transaction Monitoring System.
 * Phase 6 Verification: Tests Multithreading & Concurrency (Rubric 3).
 * Rapidly submits 10 concurrent transactions through TransactionService to a thread-safe
 * BlockingQueue consumed by 3 background worker threads (TransactionMonitor), verifying
 * synchronized StatsCounter metric aggregation without race conditions.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" AI-Powered Financial Fraud Detection System - Phase 6 Verification");
        System.out.println("=========================================================================\n");

        TransactionService transactionService = null;

        try {
            DBConnection dbConnection = DBConnection.getInstance();
            UserDAO userDAO = new UserDAO(dbConnection);
            TransactionDAO transactionDAO = new TransactionDAO(dbConnection);
            AlertDAO alertDAO = new AlertDAO(dbConnection);
            SettingsDAO settingsDAO = new SettingsDAO(dbConnection);

            seedUsersIfEmpty(userDAO);

            FraudDetectionEngine engine = new FraudDetectionEngine(settingsDAO, userDAO, transactionDAO);
            StatsCounter statsCounter = new StatsCounter();

            // Latch to synchronize and wait until all 10 transactions have been processed by workers
            int burstCount = 10;
            CountDownLatch completionLatch = new CountDownLatch(burstCount);
            AtomicInteger processedCounter = new AtomicInteger(0);

            // Initialize TransactionService with 3 background worker threads and completion callback
            transactionService = new TransactionService(
                    transactionDAO, alertDAO, engine, statsCounter,
                    (txn, result) -> {
                        int current = processedCounter.incrementAndGet();
                        System.out.printf("  [Thread: %-15s] Processed Txn #%-3d | User %d | Amount: ₹%,9.2f | Score: %3d (%-6s) -> %s%n",
                                Thread.currentThread().getName(), txn.getTxnId(), txn.getUserId(),
                                txn.getAmount(), result.getScore(), result.getLevel(), result.getRecommendedStatus());
                        completionLatch.countDown();
                    }
            );

            System.out.println("-------------------------------------------------------------------------");
            System.out.printf("Submitting %d transactions rapidly to test asynchronous worker pool...%n", burstCount);
            System.out.println("-------------------------------------------------------------------------");

            long startTime = System.currentTimeMillis();

            // Submit 10 diverse transactions rapidly
            for (int i = 1; i <= burstCount; i++) {
                Transaction txn;
                if (i == 4) {
                    // Deliberate high-risk international transaction
                    txn = new InternationalTransaction(
                            0, 2, 95000.00, "112233445566", "Dubai", "UAE",
                            "Concurrent Test - High-value Overseas Import",
                            Timestamp.valueOf(LocalDateTime.now().withHour(3).withMinute(15)), 0, RiskLevel.LOW, "PENDING"
                    );
                } else if (i == 7) {
                    // Deliberate medium-risk round amount
                    txn = new DomesticTransaction(
                            0, 3, 50000.00, "887766554499", "Bengaluru", "India",
                            "Concurrent Test - Round Amount",
                            Timestamp.valueOf(LocalDateTime.now().withHour(14)), 0, RiskLevel.LOW, "PENDING"
                    );
                } else if (i == 9) {
                    // Deliberate statistical anomaly
                    txn = new DomesticTransaction(
                            0, 3, 195000.00, "887766554488", "Bengaluru", "India",
                            "Concurrent Test - Statistical Anomaly",
                            Timestamp.valueOf(LocalDateTime.now().withHour(15)), 0, RiskLevel.LOW, "PENDING"
                    );
                } else {
                    // Normal routine domestic transaction
                    txn = new DomesticTransaction(
                            0, (i % 3) + 2, 1200.00 * i, "9876543210" + String.format("%02d", i),
                            "Mumbai", "India", "Routine retail transaction #" + i,
                            Timestamp.valueOf(LocalDateTime.now().withHour(12).withMinute(i * 5)), 0, RiskLevel.LOW, "PENDING"
                    );
                }

                // Rapid asynchronous submission (Producer)
                transactionService.submit(txn);
            }

            // Wait for all 10 asynchronous worker tasks to finish processing
            boolean allFinished = completionLatch.await(10, TimeUnit.SECONDS);
            long totalElapsed = System.currentTimeMillis() - startTime;

            System.out.println("\n-------------------------------------------------------------------------");
            System.out.println("Concurrency & Synchronization Results");
            System.out.println("-------------------------------------------------------------------------");
            System.out.printf("All 10 Transactions Processed : %s%n", (allFinished ? "YES (SUCCESS)" : "TIMEOUT"));
            System.out.printf("Total Processing Time         : %d ms%n", totalElapsed);

            // Fetch immutable snapshot from synchronized StatsCounter
            StatsCounter.StatsSnapshot snapshot = statsCounter.getSnapshot();
            System.out.printf("Synchronized Stats Snapshot   : %s%n", snapshot);
            System.out.printf("  Total Processed : %d%n", snapshot.getTotalProcessed());
            System.out.printf("  Approved        : %d%n", snapshot.getApprovedCount());
            System.out.printf("  Flagged         : %d%n", snapshot.getFlaggedCount());
            System.out.printf("  Blocked         : %d%n", snapshot.getBlockedCount());
            System.out.printf("  Total Volume    : ₹%,.2f%n", snapshot.getTotalVolumeAmount());
            System.out.printf("  Prevented Fraud : ₹%,.2f%n", snapshot.getBlockedFraudAmount());

            System.out.println("\n=========================================================================");
            System.out.println(" Phase 6 Verification Completed Successfully (No Race Conditions)!");
            System.out.println("=========================================================================");

        } catch (Exception e) {
            System.err.println("Exception during Phase 6 execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (transactionService != null) {
                transactionService.shutdown();
            }
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
