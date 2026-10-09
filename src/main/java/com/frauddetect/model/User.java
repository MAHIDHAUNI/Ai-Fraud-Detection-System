package com.frauddetect.model;

import java.sql.Timestamp;

/**
 * RUBRIC: 1 - OOP: Abstraction, Inheritance & Encapsulation
 * Abstract base class representing any authenticated system user.
 * Encapsulates common user attributes and defines abstract methods that subclasses must implement.
 */
public abstract class User {
    // Encapsulation: private attributes accessible only via public getters and setters
    private int userId;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private String email;
    private String homeCountry;
    private Timestamp createdAt;

    public User() {
        this.homeCountry = "India";
    }

    public User(int userId, String username, String fullName, String email, String homeCountry) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.homeCountry = (homeCountry != null && !homeCountry.isBlank()) ? homeCountry : "India";
    }

    public User(int userId, String username, String passwordHash, String salt,
                String fullName, String email, String homeCountry, Timestamp createdAt) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.fullName = fullName;
        this.email = email;
        this.homeCountry = (homeCountry != null && !homeCountry.isBlank()) ? homeCountry : "India";
        this.createdAt = createdAt;
    }

    // Abstraction & Polymorphism: subclasses must provide their own role and dashboard header
    public abstract String getRole();
    public abstract String getDashboardTitle();

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHomeCountry() {
        return homeCountry;
    }

    public void setHomeCountry(String homeCountry) {
        this.homeCountry = homeCountry;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%d, username='%s', name='%s', role='%s']",
                getClass().getSimpleName(), userId, username, fullName, getRole());
    }
}
