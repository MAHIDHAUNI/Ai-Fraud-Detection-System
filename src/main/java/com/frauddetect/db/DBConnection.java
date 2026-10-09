package com.frauddetect.db;

import com.frauddetect.exception.DatabaseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * RUBRIC: 5 - Database connectivity (JDBC)
 * Singleton connection manager for MySQL database.
 * Thread-safe implementation that loads credentials securely from config.properties.
 * Wraps low-level SQLExceptions into domain-specific DatabaseExceptions.
 */
public class DBConnection {

    private static volatile DBConnection instance;
    private final String url;
    private final String user;
    private final String password;

    // Private constructor enforcing the Singleton pattern
    private DBConnection() throws DatabaseException {
        Properties props = loadConfiguration();
        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password");

        if (this.url == null || this.user == null) {
            throw new DatabaseException("Database configuration missing required 'db.url' or 'db.user' properties.");
        }

        // Test driver availability
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("MySQL JDBC Driver 'com.mysql.cj.jdbc.Driver' not found on classpath.", e);
        }
    }

    /**
     * Double-checked locking thread-safe singleton accessor.
     *
     * @return the singleton instance of DBConnection
     * @throws DatabaseException if configuration fails to load
     */
    public static DBConnection getInstance() throws DatabaseException {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Establishes and returns a new JDBC Connection to MySQL.
     *
     * @return active java.sql.Connection
     * @throws DatabaseException if connection cannot be established
     */
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish database connection to: " + url +
                    ". Please verify MySQL is running and credentials in config.properties are correct.", e);
        }
    }

    /**
     * Verifies connectivity by attempting a connection.
     *
     * @return true if database connection succeeds
     * @throws DatabaseException if connection fails
     */
    public boolean testConnection() throws DatabaseException {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            throw new DatabaseException("Database ping check failed.", e);
        }
    }

    /**
     * Securely loads configuration properties from classpath or fallback file paths.
     */
    private Properties loadConfiguration() throws DatabaseException {
        Properties properties = new Properties();
        String configFileName = "config.properties";

        // 1. Attempt to load from Thread Context ClassLoader
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName)) {
            if (in != null) {
                properties.load(in);
                return properties;
            }
        } catch (IOException e) {
            throw new DatabaseException("Failed to read " + configFileName + " from classpath.", e);
        }

        // 2. Attempt to load from class-relative resource
        try (InputStream in = DBConnection.class.getResourceAsStream("/" + configFileName)) {
            if (in != null) {
                properties.load(in);
                return properties;
            }
        } catch (IOException e) {
            throw new DatabaseException("Failed to read " + configFileName + " from root classpath.", e);
        }

        // 3. Fallback: file system check (project root or src/main/resources)
        File[] candidateFiles = new File[] {
                new File(configFileName),
                new File("src/main/resources", configFileName)
        };

        for (File candidate : candidateFiles) {
            if (candidate.exists()) {
                try (InputStream in = new FileInputStream(candidate)) {
                    properties.load(in);
                    return properties;
                } catch (IOException e) {
                    throw new DatabaseException("Failed to read configuration file: " + candidate.getAbsolutePath(), e);
                }
            }
        }

        throw new DatabaseException("Configuration file '" + configFileName + "' not found. " +
                "Please copy config.properties.example to config.properties and provide your database credentials.");
    }
}
