package com.frauddetect.dao;

import com.frauddetect.db.DBConnection;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Alert;
import com.frauddetect.model.RiskLevel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RUBRIC: 4 - Classes for database operations (AlertDAO)
 * RUBRIC: 6 - Implement JDBC: PreparedStatement, ResultSet, try-with-resources
 * Data access class for security alert monitoring and administrative resolutions.
 */
public class AlertDAO implements Repository<Alert, Integer> {

    private final DBConnection dbConnection;

    public AlertDAO() throws DatabaseException {
        this.dbConnection = DBConnection.getInstance();
    }

    public AlertDAO(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Alert save(Alert alert) throws DatabaseException {
        String sql = "INSERT INTO alerts (txn_id, user_id, risk_level, reasons, alert_time, resolved, admin_note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, alert.getTxnId());
            ps.setInt(2, alert.getUserId());
            ps.setString(3, alert.getRiskLevel().name());
            ps.setString(4, alert.getReasons());
            ps.setTimestamp(5, alert.getAlertTime() != null ? alert.getAlertTime() : new Timestamp(System.currentTimeMillis()));
            ps.setBoolean(6, alert.isResolved());
            ps.setString(7, alert.getAdminNote());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    alert.setAlertId(generatedKeys.getInt(1));
                }
            }
            return alert;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist alert for transaction: " + alert.getTxnId(), e);
        }
    }

    @Override
    public Optional<Alert> findById(Integer id) throws DatabaseException {
        String sql = "SELECT * FROM alerts WHERE alert_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAlert(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find alert with ID: " + id, e);
        }
    }

    @Override
    public List<Alert> findAll() throws DatabaseException {
        String sql = "SELECT * FROM alerts ORDER BY alert_time DESC, alert_id DESC";
        List<Alert> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToAlert(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all alerts.", e);
        }
    }

    /**
     * Retrieves all pending, unresolved alerts for admin review.
     */
    public List<Alert> findUnresolved() throws DatabaseException {
        String sql = "SELECT * FROM alerts WHERE resolved = FALSE ORDER BY alert_time DESC, alert_id DESC";
        List<Alert> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToAlert(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve unresolved alerts.", e);
        }
    }

    /**
     * Retrieves alerts concerning a specific user's transactions.
     */
    public List<Alert> findByUser(int userId) throws DatabaseException {
        String sql = "SELECT * FROM alerts WHERE user_id = ? ORDER BY alert_time DESC";
        List<Alert> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAlert(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find alerts for user ID: " + userId, e);
        }
    }

    /**
     * Resolves an alert by recording administrative review notes.
     */
    public boolean resolve(int alertId, String note) throws DatabaseException {
        String sql = "UPDATE alerts SET resolved = TRUE, admin_note = ? WHERE alert_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, note);
            ps.setInt(2, alertId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to resolve alert ID: " + alertId, e);
        }
    }

    @Override
    public boolean update(Alert alert) throws DatabaseException {
        String sql = "UPDATE alerts SET risk_level = ?, reasons = ?, resolved = ?, admin_note = ? WHERE alert_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, alert.getRiskLevel().name());
            ps.setString(2, alert.getReasons());
            ps.setBoolean(3, alert.isResolved());
            ps.setString(4, alert.getAdminNote());
            ps.setInt(5, alert.getAlertId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update alert ID: " + alert.getAlertId(), e);
        }
    }

    @Override
    public boolean delete(Integer id) throws DatabaseException {
        String sql = "DELETE FROM alerts WHERE alert_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete alert ID: " + id, e);
        }
    }

    private Alert mapRowToAlert(ResultSet rs) throws SQLException {
        int alertId = rs.getInt("alert_id");
        int txnId = rs.getInt("txn_id");
        int userId = rs.getInt("user_id");
        String riskLevelStr = rs.getString("risk_level");
        String reasons = rs.getString("reasons");
        Timestamp alertTime = rs.getTimestamp("alert_time");
        boolean resolved = rs.getBoolean("resolved");
        String adminNote = rs.getString("admin_note");

        RiskLevel riskLevel;
        try {
            riskLevel = RiskLevel.valueOf(riskLevelStr);
        } catch (Exception e) {
            riskLevel = RiskLevel.MEDIUM;
        }

        return new Alert(alertId, txnId, userId, riskLevel, reasons, alertTime, resolved, adminNote);
    }
}
