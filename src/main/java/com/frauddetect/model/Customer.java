package com.frauddetect.model;

import java.sql.Timestamp;

/**
 * RUBRIC: 1 - OOP: Inheritance & Polymorphism
 * Subclass of User representing retail customers submitting transactions.
 * Overrides getRole() and getDashboardTitle() polymorphically.
 */
public class Customer extends User {

    public Customer() {
        super();
    }

    public Customer(int userId, String username, String fullName, String email, String homeCountry) {
        super(userId, username, fullName, email, homeCountry);
    }

    public Customer(int userId, String username, String passwordHash, String salt,
                    String fullName, String email, String homeCountry, Timestamp createdAt) {
        super(userId, username, passwordHash, salt, fullName, email, homeCountry, createdAt);
    }

    // Polymorphism: Concrete implementation for Customer role
    @Override
    public String getRole() {
        return "CUSTOMER";
    }

    // Polymorphism: Custom dashboard title for Customer UI
    @Override
    public String getDashboardTitle() {
        return "Customer Dashboard - Welcome, " + getFullName();
    }
}
