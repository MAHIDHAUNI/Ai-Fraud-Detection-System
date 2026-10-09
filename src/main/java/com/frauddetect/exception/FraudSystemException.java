package com.frauddetect.exception;

/**
 * RUBRIC: 1 - OOP: Exception Handling & Inheritance
 * Base checked exception class for all business and domain errors in the fraud detection system.
 * Demonstrates a custom exception hierarchy extending java.lang.Exception.
 */
public class FraudSystemException extends Exception {

    public FraudSystemException(String message) {
        super(message);
    }

    public FraudSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
