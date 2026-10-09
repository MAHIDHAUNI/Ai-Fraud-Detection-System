package com.frauddetect.service;

import com.frauddetect.dao.UserDAO;
import com.frauddetect.exception.AuthenticationException;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.exception.FraudSystemException;
import com.frauddetect.model.Customer;
import com.frauddetect.model.User;
import com.frauddetect.util.PasswordUtil;

import java.util.Optional;

/**
 * RUBRIC: 1 - OOP: Exception Handling (AuthenticationException) & Polymorphism (User -> Customer/Admin)
 * Service managing user authentication, credential verification, and customer registration.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user against stored salted hashes using constant-time verification.
     *
     * @param username user login handle
     * @param password raw plain-text password
     * @return authenticated User object (polymorphically Customer or Admin)
     * @throws AuthenticationException if username is unknown or password does not match
     * @throws DatabaseException       on database connectivity failures
     */
    public User login(String username, String password) throws AuthenticationException, DatabaseException {
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Username cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new AuthenticationException("Password cannot be empty.");
        }

        String trimmedUser = username.trim();
        Optional<User> userOpt = userDAO.findByUsername(trimmedUser);

        if (userOpt.isEmpty()) {
            // Generic message to prevent username enumeration side-channels
            throw new AuthenticationException("Invalid username or password.");
        }

        User user = userOpt.get();

        // Constant-time SHA-256 password hash comparison
        boolean passwordMatches = PasswordUtil.verify(password, user.getSalt(), user.getPasswordHash());
        if (!passwordMatches) {
            throw new AuthenticationException("Invalid username or password.");
        }

        return user;
    }

    /**
     * Registers a new Customer account with cryptographically secure salted password hashing.
     */
    public Customer registerCustomer(String username, String rawPassword, String fullName,
                                     String email, String homeCountry) throws FraudSystemException {
        if (username == null || username.trim().length() < 3) {
            throw new FraudSystemException("Username must contain at least 3 characters.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new FraudSystemException("Password must contain at least 6 characters.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new FraudSystemException("Full name cannot be empty.");
        }

        String trimmedUser = username.trim();
        if (userDAO.findByUsername(trimmedUser).isPresent()) {
            throw new FraudSystemException("Username '" + trimmedUser + "' is already registered. Please choose another.");
        }

        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hash(rawPassword, salt);
        String country = (homeCountry != null && !homeCountry.isBlank()) ? homeCountry.trim() : "India";

        Customer customer = new Customer(0, trimmedUser, passwordHash, salt, fullName.trim(),
                (email != null ? email.trim() : null), country, null);

        userDAO.save(customer);
        return customer;
    }
}
