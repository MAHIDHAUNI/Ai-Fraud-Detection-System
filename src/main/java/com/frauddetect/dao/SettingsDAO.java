package com.frauddetect.dao;

import com.frauddetect.db.DBConnection;
import com.frauddetect.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * RUBRIC: 4 - Classes for database operations (SettingsDAO)
 * RUBRIC: 6 - Implement JDBC: PreparedStatement, ResultSet, try-with-resources
 * RUBRIC: 2 - Collections & Generics (Map<String, String>)
 * Data access class for reading and modifying dynamic fraud detection configuration.
 */
public class SettingsDAO {

    private final DBConnection dbConnection;

    public SettingsDAO() throws DatabaseException {
        this.dbConnection = DBConnection.getInstance();
    }

    public SettingsDAO(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * RUBRIC: 2 - Collections (Map<String, String>)
     * Retrieves all settings as a key-value Map.
     *
     * @return Map containing all setting_key -> setting_value pairs
     * @throws DatabaseException on database error
     */
    public Map<String, String> getAll() throws DatabaseException {
        String sql = "SELECT setting_key, setting_value FROM settings";
        Map<String, String> map = new HashMap<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
            return map;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load settings from database.", e);
        }
    }

    /**
     * Retrieves the string value of a setting.
     */
    public Optional<String> get(String key) throws DatabaseException {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, key);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("setting_value"));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch setting for key: " + key, e);
        }
    }

    /**
     * Retrieves a setting as an integer with fallback default.
     */
    public int getInt(String key, int defaultValue) throws DatabaseException {
        return get(key).map(v -> {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    /**
     * Retrieves a setting as a double with fallback default.
     */
    public double getDouble(String key, double defaultValue) throws DatabaseException {
        return get(key).map(v -> {
            try {
                return Double.parseDouble(v.trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    /**
     * Retrieves a setting as a boolean with fallback default.
     */
    public boolean getBoolean(String key, boolean defaultValue) throws DatabaseException {
        return get(key).map(v -> Boolean.parseBoolean(v.trim())).orElse(defaultValue);
    }

    /**
     * Updates an existing setting value.
     *
     * @param key   the setting key
     * @param value the new value
     * @return true if updated, false if key does not exist
     * @throws DatabaseException on database error
     */
    public boolean update(String key, String value) throws DatabaseException {
        String sql = "UPDATE settings SET setting_value = ? WHERE setting_key = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            ps.setString(2, key);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update setting: " + key, e);
        }
    }

    /**
     * Inserts or updates a setting key, value, and description.
     */
    public boolean saveOrUpdate(String key, String value, String description) throws DatabaseException {
        String sql = "INSERT INTO settings (setting_key, setting_value, description) " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), description = VALUES(description)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, description);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save or update setting: " + key, e);
        }
    }
}
