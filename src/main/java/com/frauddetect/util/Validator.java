package com.frauddetect.util;

import com.frauddetect.exception.InvalidTransactionException;
import com.frauddetect.model.Transaction;

import java.util.regex.Pattern;

/**
 * Utility class providing validation for transactions and user inputs.
 * Ensures data integrity before transactions are persisted or analyzed.
 */
public final class Validator {

    public static final double MIN_TRANSACTION_AMOUNT = 0.01;
    public static final double MAX_TRANSACTION_AMOUNT = 10_000_000.00; // 1 Crore INR limit

    // Account numbers must contain between 8 and 18 numeric digits
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{8,18}$");

    private Validator() {
        // Prevent instantiation
    }

    /**
     * Validates an entire Transaction object against business criteria.
     *
     * @param transaction the transaction to validate
     * @throws InvalidTransactionException if any field is invalid
     */
    public static void validateTransaction(Transaction transaction) throws InvalidTransactionException {
        if (transaction == null) {
            throw new InvalidTransactionException("Transaction cannot be null.");
        }
        validateAmount(transaction.getAmount());
        validateReceiverAccount(transaction.getReceiverAccount());
        validateLocation(transaction.getLocation());
        validateCountry(transaction.getCountry());
    }

    /**
     * Validates transaction amount (> 0 and <= 10,000,000).
     *
     * @param amount the transaction amount
     * @throws InvalidTransactionException if outside allowed range
     */
    public static void validateAmount(double amount) throws InvalidTransactionException {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new InvalidTransactionException("Transaction amount is not a valid number.");
        }
        if (amount < MIN_TRANSACTION_AMOUNT) {
            throw new InvalidTransactionException(
                    String.format("Invalid transaction amount: ₹%.2f. Amount must be strictly greater than zero.", amount));
        }
        if (amount > MAX_TRANSACTION_AMOUNT) {
            throw new InvalidTransactionException(
                    String.format("Invalid transaction amount: ₹%.2f. Maximum allowed limit per transfer is ₹%,.2f.",
                            amount, MAX_TRANSACTION_AMOUNT));
        }
    }

    /**
     * Validates receiver account format (digits only, 8 to 18 characters).
     *
     * @param receiverAccount the account number
     * @throws InvalidTransactionException if null, incorrect length, or non-numeric
     */
    public static void validateReceiverAccount(String receiverAccount) throws InvalidTransactionException {
        if (receiverAccount == null || receiverAccount.trim().isEmpty()) {
            throw new InvalidTransactionException("Receiver account number cannot be empty.");
        }
        String trimmed = receiverAccount.trim();
        if (!ACCOUNT_NUMBER_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidTransactionException(
                    "Invalid receiver account '" + receiverAccount + "'. Account number must contain 8 to 18 numeric digits.");
        }
    }

    /**
     * Validates that location is specified.
     *
     * @param location the transaction city/location
     * @throws InvalidTransactionException if empty
     */
    public static void validateLocation(String location) throws InvalidTransactionException {
        if (location == null || location.trim().isEmpty()) {
            throw new InvalidTransactionException("Transaction location cannot be empty.");
        }
    }

    /**
     * Validates that country is specified.
     *
     * @param country the transaction country
     * @throws InvalidTransactionException if empty
     */
    public static void validateCountry(String country) throws InvalidTransactionException {
        if (country == null || country.trim().isEmpty()) {
            throw new InvalidTransactionException("Transaction country cannot be empty.");
        }
    }
}
