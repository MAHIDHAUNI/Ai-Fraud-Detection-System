package com.frauddetect.model;

import java.sql.Timestamp;

/**
 * RUBRIC: 1 - OOP: Inheritance & Polymorphism
 * Subclass of User representing security analysts and administrators.
 * Overrides getRole() and getDashboardTitle() polymorphically.
 */
public class Admin extends User {

    public Admin() {
        super();
    }

    public Admin(int userId, String username, String fullName, String email, String homeCountry) {
        super(userId, username, fullName, email, homeCountry);
    }

    public Admin(int userId, String username, String passwordHash, String salt,
                 String fullName, String email, String homeCountry, Timestamp createdAt) {
        super(userId, username, passwordHash, salt, fullName, email, homeCountry, createdAt);
    }

    // Polymorphism: Concrete implementation for Admin role
    @Override
    public String getRole() {
        return "ADMIN";
    }

    // Polymorphism: Custom dashboard title for Admin UI
    @Override
    public String getDashboardTitle() {
        return "Admin Security Console - Operator: " + getFullName();
    }
}
