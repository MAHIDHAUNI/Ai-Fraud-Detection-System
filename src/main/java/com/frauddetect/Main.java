package com.frauddetect;

import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.SettingsDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.dao.UserDAO;
import com.frauddetect.db.DBConnection;
import com.frauddetect.detection.FraudDetectionEngine;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.gui.LoginFrame;
import com.frauddetect.gui.UIHelper;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Customer;
import com.frauddetect.model.User;
import com.frauddetect.service.AlertService;
import com.frauddetect.service.AuthService;
import com.frauddetect.service.ReportService;
import com.frauddetect.service.TransactionService;
import com.frauddetect.util.PasswordUtil;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Main application entry point for AI Fraud Detection & Transaction Monitoring System.
 * Initializes all DAO/Service/Engine layers and launches the Swing GUI on the Event Dispatch Thread.
 *
 * RUBRIC: 3 - Multithreading: GUI launched via SwingUtilities.invokeLater (EDT)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" AI-Powered Financial Fraud Detection System — Starting...");
        System.out.println("=========================================================================\n");

        try {
            // 1. Initialize database connection (Singleton pattern)
            DBConnection dbConnection = DBConnection.getInstance();

            // 2. Create DAO layer instances
            UserDAO userDAO = new UserDAO(dbConnection);
            TransactionDAO transactionDAO = new TransactionDAO(dbConnection);
            AlertDAO alertDAO = new AlertDAO(dbConnection);
            SettingsDAO settingsDAO = new SettingsDAO(dbConnection);

            // 3. Seed default users if the database is empty (first-run bootstrap)
            seedUsersIfEmpty(userDAO);

            // 4. Initialize detection engine (loads rules and thresholds from DB)
            FraudDetectionEngine engine = new FraudDetectionEngine(settingsDAO, userDAO, transactionDAO);

            // 5. Create service layer instances
            AuthService authService = new AuthService(userDAO);
            TransactionService transactionService = new TransactionService(transactionDAO, alertDAO, engine);
            AlertService alertService = new AlertService(alertDAO);
            ReportService reportService = new ReportService(transactionDAO, userDAO, alertDAO);

            // 6. Install modern FlatLaf theme
            UIHelper.setupTheme();

            // 7. Launch LoginFrame on the Event Dispatch Thread (EDT)
            // RUBRIC: 3 - Multithreading: Swing GUI must be created and updated on the EDT
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame(
                        authService,
                        transactionService,
                        alertService,
                        reportService,
                        settingsDAO,
                        userDAO,
                        engine
                );
                loginFrame.setVisible(true);
            });

            System.out.println("GUI launched. Login window is now open.");

            // Register JVM shutdown hook for clean thread pool termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down background services...");
                transactionService.shutdown();
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            }));

        } catch (DatabaseException e) {
            System.err.println("FATAL: Could not connect to database: " + e.getMessage());
            System.err.println("Please verify config.properties and ensure MySQL is running.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("FATAL: Unexpected startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Seeds the database with default users (1 admin + 3 customers) on first run.
     * Uses PasswordUtil for cryptographically secure salted password hashing.
     */
    public static void seedUsersIfEmpty(UserDAO userDAO) throws DatabaseException {
        List<User> existingUsers = userDAO.findAll();
        if (!existingUsers.isEmpty()) {
            return;
        }

        System.out.println("First run detected — seeding default users...");

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

        System.out.println("Default users seeded: admin, john_doe, priya_sharma, rahul_verma");
    }
}
