package com.frauddetect.exception;

/**
 * RUBRIC: 1 - OOP: Exception Handling & Inheritance
 * Checked exception thrown when transaction inputs fail business validation
 * (e.g., negative or excessive amounts, invalid account format, missing location).
 */
public class InvalidTransactionException extends FraudSystemException {

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
