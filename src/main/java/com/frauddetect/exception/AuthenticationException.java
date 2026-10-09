package com.frauddetect.exception;

/**
 * RUBRIC: 1 - OOP: Exception Handling & Inheritance
 * Checked exception thrown when user authentication fails due to incorrect credentials
 * or non-existent usernames.
 */
public class AuthenticationException extends FraudSystemException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
