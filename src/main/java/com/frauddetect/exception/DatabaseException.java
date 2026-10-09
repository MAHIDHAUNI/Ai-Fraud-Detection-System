package com.frauddetect.exception;

/**
 * RUBRIC: 1 - OOP: Exception Handling & Inheritance
 * Checked exception wrapping underlying SQL and database connectivity failures,
 * preventing low-level JDBC exceptions from leaking directly into presentation layers.
 */
public class DatabaseException extends FraudSystemException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
