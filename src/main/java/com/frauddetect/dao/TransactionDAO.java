package com.frauddetect.dao;

import com.frauddetect.db.DBConnection;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Alert;
import com.frauddetect.model.DomesticTransaction;
import com.frauddetect.model.InternationalTransaction;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RUBRIC: 4 - Classes for database operations (TransactionDAO)
 * RUBRIC: 6 - Implement JDBC: PreparedStatement, ResultSets, try-with-resources,
 *             and database transaction management (setAutoCommit(false), commit, rollback).
 * RUBRIC: 1 - OOP: Polymorphic object instantiation (Domestic vs International)
 * RUBRIC: 2 - Collections & Generics (List, Set, Optional)
 */
public class TransactionDAO implements Repository<Transaction, Integer> {

    private final DBConnection dbConnection;

    /**
     * Data Transfer Object for statistical anomaly detection.
     */
    public static class AmountStats {
        private final double mean;
        private final double stdDev;
        private final int count;

        public AmountStats(double mean, double stdDev, int count) {
            this.mean = mean;
            this.stdDev = stdDev;
            this.count = count;
        }

        public double getMean() {
            return mean;
        }

        public double getStdDev() {
            return stdDev;
        }

        public int getCount() {
            return count;
        }

        @Override
        public String toString() {
            return String.format("AmountStats[mean=₹%.2f, stdDev=₹%.2f, count=%d]", mean, stdDev, count);
        }
    }

    public TransactionDAO() throws DatabaseException {
        this.dbConnection = DBConnection.getInstance();
    }

    public TransactionDAO(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Transaction save(Transaction txn) throws DatabaseException {
        String sql = "INSERT INTO transactions " +
                     "(user_id, amount, txn_type, receiver_account, location, country, description, txn_time, risk_score, risk_level, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setTransactionParameters(ps, txn);
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    txn.setTxnId(generatedKeys.getInt(1));
                }
            }
            return txn;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist transaction for user: " + txn.getUserId(), e);
        }
    }

    /**
     * RUBRIC: 6 - Atomically saves a transaction and its accompanying alert within a single DB transaction.
     * Demonstrates JDBC transaction control: setAutoCommit(false), commit(), rollback().
     *
     * @param txn   the transaction to persist
     * @param alert the alert to persist (alert's txnId is linked automatically)
     * @throws DatabaseException if the transaction fails and is rolled back
     */
    public void saveWithAlert(Transaction txn, Alert alert) throws DatabaseException {
        String txnSql = "INSERT INTO transactions " +
                        "(user_id, amount, txn_type, receiver_account, location, country, description, txn_time, risk_score, risk_level, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String alertSql = "INSERT INTO alerts (txn_id, user_id, risk_level, reasons, alert_time, resolved, admin_note) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false); // Begin database transaction

            // 1. Insert Transaction
            try (PreparedStatement psTxn = conn.prepareStatement(txnSql, Statement.RETURN_GENERATED_KEYS)) {
                setTransactionParameters(psTxn, txn);
                psTxn.executeUpdate();

                try (ResultSet keys = psTxn.getGeneratedKeys()) {
                    if (keys.next()) {
                        txn.setTxnId(keys.getInt(1));
                        alert.setTxnId(txn.getTxnId());
                    }
                }
            }

            // 2. Insert Alert
            try (PreparedStatement psAlert = conn.prepareStatement(alertSql, Statement.RETURN_GENERATED_KEYS)) {
                psAlert.setInt(1, alert.getTxnId());
                psAlert.setInt(2, alert.getUserId());
                psAlert.setString(3, alert.getRiskLevel().name());
                psAlert.setString(4, alert.getReasons());
                psAlert.setTimestamp(5, alert.getAlertTime() != null ? alert.getAlertTime() : new Timestamp(System.currentTimeMillis()));
                psAlert.setBoolean(6, alert.isResolved());
                psAlert.setString(7, alert.getAdminNote());

                psAlert.executeUpdate();

                try (ResultSet alertKeys = psAlert.getGeneratedKeys()) {
                    if (alertKeys.next()) {
                        alert.setAlertId(alertKeys.getInt(1));
                    }
                }
            }

            conn.commit(); // Commit database transaction
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on failure
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
            }
            throw new DatabaseException("Transaction and alert rollback occurred due to error: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public Optional<Transaction> findById(Integer id) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE txn_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTransaction(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find transaction by ID: " + id, e);
        }
    }

    @Override
    public List<Transaction> findAll() throws DatabaseException {
        String sql = "SELECT * FROM transactions ORDER BY txn_time DESC, txn_id DESC";
        List<Transaction> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToTransaction(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all transactions.", e);
        }
    }

    /**
     * Retrieves all transactions executed by a specific customer.
     */
    public List<Transaction> findByUser(int userId) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY txn_time DESC, txn_id DESC";
        List<Transaction> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find transactions for user ID: " + userId, e);
        }
    }

    /**
     * Retrieves transactions performed by the user within the last N minutes.
     * Used for velocity rule and rapid repetition checks.
     */
    public List<Transaction> findRecentByUser(int userId, int minutes) throws DatabaseException {
        String sql = "SELECT * FROM transactions " +
                     "WHERE user_id = ? AND txn_time >= DATE_SUB(NOW(), INTERVAL ? MINUTE) " +
                     "ORDER BY txn_time DESC";
        List<Transaction> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, minutes);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find recent transactions for user: " + userId, e);
        }
    }

    /**
     * Computes the mean (average) and standard deviation of historical transaction amounts for a user.
     * Used by the Statistical Anomaly Detection rule (z-score calculation).
     */
    public AmountStats getAmountStats(int userId) throws DatabaseException {
        String sql = "SELECT COALESCE(AVG(amount), 0.0) AS avg_amt, " +
                     "       COALESCE(STDDEV(amount), 0.0) AS std_amt, " +
                     "       COUNT(*) AS total_count " +
                     "FROM transactions " +
                     "WHERE user_id = ? AND status != 'BLOCKED'";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double mean = rs.getDouble("avg_amt");
                    double stdDev = rs.getDouble("std_amt");
                    int count = rs.getInt("total_count");
                    return new AmountStats(mean, stdDev, count);
                }
            }
            return new AmountStats(0.0, 0.0, 0);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to compute amount statistics for user: " + userId, e);
        }
    }

    /**
     * RUBRIC: 2 - Collections (Set<String>)
     * Returns the distinct set of countries where this user has executed transactions historically.
     */
    public Set<String> getUserCountries(int userId) throws DatabaseException {
        String sql = "SELECT DISTINCT country FROM transactions WHERE user_id = ? AND country IS NOT NULL";
        Set<String> countries = new HashSet<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String c = rs.getString("country");
                    if (c != null && !c.isBlank()) {
                        countries.add(c.trim());
                    }
                }
            }
            return countries;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get countries for user ID: " + userId, e);
        }
    }

    /**
     * Updates the calculated risk score, risk level, and final decision status of a transaction.
     */
    public boolean updateRisk(int txnId, int score, RiskLevel level, String status) throws DatabaseException {
        String sql = "UPDATE transactions SET risk_score = ?, risk_level = ?, status = ? WHERE txn_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, score);
            ps.setString(2, level.name());
            ps.setString(3, status);
            ps.setInt(4, txnId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update risk parameters for transaction: " + txnId, e);
        }
    }

    @Override
    public boolean update(Transaction txn) throws DatabaseException {
        String sql = "UPDATE transactions SET amount = ?, receiver_account = ?, location = ?, " +
                     "country = ?, description = ?, risk_score = ?, risk_level = ?, status = ? " +
                     "WHERE txn_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, txn.getAmount());
            ps.setString(2, txn.getReceiverAccount());
            ps.setString(3, txn.getLocation());
            ps.setString(4, txn.getCountry());
            ps.setString(5, txn.getDescription());
            ps.setInt(6, txn.getRiskScore());
            ps.setString(7, txn.getRiskLevel().name());
            ps.setString(8, txn.getStatus());
            ps.setInt(9, txn.getTxnId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update transaction: " + txn.getTxnId(), e);
        }
    }

    @Override
    public boolean delete(Integer id) throws DatabaseException {
        String sql = "DELETE FROM transactions WHERE txn_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete transaction: " + id, e);
        }
    }

    /**
     * Helper to bind transaction parameters to a PreparedStatement.
     */
    private void setTransactionParameters(PreparedStatement ps, Transaction txn) throws SQLException {
        ps.setInt(1, txn.getUserId());
        ps.setDouble(2, txn.getAmount());
        ps.setString(3, txn.getTxnType());
        ps.setString(4, txn.getReceiverAccount());
        ps.setString(5, txn.getLocation());
        ps.setString(6, txn.getCountry());
        ps.setString(7, txn.getDescription());
        ps.setTimestamp(8, txn.getTxnTime() != null ? txn.getTxnTime() : new Timestamp(System.currentTimeMillis()));
        ps.setInt(9, txn.getRiskScore());
        ps.setString(10, txn.getRiskLevel() != null ? txn.getRiskLevel().name() : RiskLevel.LOW.name());
        ps.setString(11, txn.getStatus() != null ? txn.getStatus() : "PENDING");
    }

    /**
     * Polymorphically maps a database row to DomesticTransaction or InternationalTransaction.
     */
    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        int txnId = rs.getInt("txn_id");
        int userId = rs.getInt("user_id");
        double amount = rs.getDouble("amount");
        String txnType = rs.getString("txn_type");
        String receiverAccount = rs.getString("receiver_account");
        String location = rs.getString("location");
        String country = rs.getString("country");
        String description = rs.getString("description");
        Timestamp txnTime = rs.getTimestamp("txn_time");
        int riskScore = rs.getInt("risk_score");
        String riskLevelStr = rs.getString("risk_level");
        String status = rs.getString("status");

        RiskLevel riskLevel;
        try {
            riskLevel = RiskLevel.valueOf(riskLevelStr);
        } catch (Exception e) {
            riskLevel = RiskLevel.LOW;
        }

        // Polymorphism: instantiate DomesticTransaction or InternationalTransaction
        if ("INTERNATIONAL".equalsIgnoreCase(txnType)) {
            return new InternationalTransaction(txnId, userId, amount, receiverAccount,
                    location, country, description, txnTime, riskScore, riskLevel, status);
        } else {
            return new DomesticTransaction(txnId, userId, amount, receiverAccount,
                    location, country, description, txnTime, riskScore, riskLevel, status);
        }
    }
}
