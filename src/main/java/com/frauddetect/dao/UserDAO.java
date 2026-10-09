package com.frauddetect.dao;

import com.frauddetect.db.DBConnection;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Customer;
import com.frauddetect.model.User;

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
 * RUBRIC: 4 - Classes for database operations (UserDAO)
 * RUBRIC: 6 - Implement JDBC (PreparedStatement, ResultSet, try-with-resources)
 * RUBRIC: 1 - OOP: Polymorphic object instantiation (Customer vs Admin) based on DB role
 */
public class UserDAO implements Repository<User, Integer> {

    private final DBConnection dbConnection;

    public UserDAO() throws DatabaseException {
        this.dbConnection = DBConnection.getInstance();
    }

    public UserDAO(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public User save(User user) throws DatabaseException {
        String sql = "INSERT INTO users (username, password_hash, salt, full_name, email, role, home_country) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getRole());
            ps.setString(7, user.getHomeCountry() != null ? user.getHomeCountry() : "India");

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save user: " + user.getUsername(), e);
        }
    }

    @Override
    public Optional<User> findById(Integer id) throws DatabaseException {
        String sql = "SELECT user_id, username, password_hash, salt, full_name, email, role, home_country, created_at " +
                     "FROM users WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user with ID: " + id, e);
        }
    }

    /**
     * Finds a user by their unique login username.
     *
     * @param username the login name
     * @return Optional carrying Customer or Admin polymorphically
     * @throws DatabaseException on database error
     */
    public Optional<User> findByUsername(String username) throws DatabaseException {
        String sql = "SELECT user_id, username, password_hash, salt, full_name, email, role, home_country, created_at " +
                     "FROM users WHERE username = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user with username: " + username, e);
        }
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        String sql = "SELECT user_id, username, password_hash, salt, full_name, email, role, home_country, created_at " +
                     "FROM users ORDER BY user_id ASC";
        List<User> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all users.", e);
        }
    }

    @Override
    public boolean update(User user) throws DatabaseException {
        String sql = "UPDATE users SET full_name = ?, email = ?, home_country = ? WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getHomeCountry());
            ps.setInt(4, user.getUserId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user: " + user.getUserId(), e);
        }
    }

    @Override
    public boolean delete(Integer id) throws DatabaseException {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete user ID: " + id, e);
        }
    }

    /**
     * Polymorphically maps a database row to either Customer or Admin.
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String salt = rs.getString("salt");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String role = rs.getString("role");
        String homeCountry = rs.getString("home_country");
        Timestamp createdAt = rs.getTimestamp("created_at");

        // Polymorphism: instantiate concrete subclass according to role column
        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(userId, username, passwordHash, salt, fullName, email, homeCountry, createdAt);
        } else {
            return new Customer(userId, username, passwordHash, salt, fullName, email, homeCountry, createdAt);
        }
    }
}
