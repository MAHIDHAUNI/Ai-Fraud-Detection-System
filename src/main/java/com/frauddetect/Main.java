package com.frauddetect;

import com.frauddetect.exception.InvalidTransactionException;
import com.frauddetect.model.Admin;
import com.frauddetect.model.Customer;
import com.frauddetect.model.DomesticTransaction;
import com.frauddetect.model.InternationalTransaction;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;
import com.frauddetect.model.User;
import com.frauddetect.util.PasswordUtil;
import com.frauddetect.util.Result;
import com.frauddetect.util.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application entry point for AI Fraud Detection & Transaction Monitoring System.
 * Phase 3 Verification: Tests PasswordUtil (SHA-256 + salt + constant-time verify),
 * Validator input checks (amount & account validation), custom exceptions, and Result<T>.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" AI-Powered Financial Fraud Detection System - Phase 3 Verification");
        System.out.println("=========================================================================\n");

        // -----------------------------------------------------------------
        // 1. PasswordUtil Verification (SecureRandom, SHA-256, Constant-Time Verify)
        // -----------------------------------------------------------------
        System.out.println("--- 1. Password Security (PasswordUtil Verification) ---");
        String rawPassword = "admin123";
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hash(rawPassword, salt);

        System.out.println("Raw Password   : " + rawPassword);
        System.out.println("Generated Salt : " + salt + " (Length: " + salt.length() + ")");
        System.out.println("Hashed Password: " + hashedPassword + " (Length: " + hashedPassword.length() + ")");

        boolean correctPassCheck = PasswordUtil.verify(rawPassword, salt, hashedPassword);
        boolean wrongPassCheck = PasswordUtil.verify("wrong_password", salt, hashedPassword);

        System.out.println("Verify with correct password ('admin123') : " + (correctPassCheck ? "PASSED (true)" : "FAILED"));
        System.out.println("Verify with incorrect password ('wrong')    : " + (!wrongPassCheck ? "PASSED (false - rejected)" : "FAILED"));

        // -----------------------------------------------------------------
        // 2. Validator & Custom Exception Verification
        // -----------------------------------------------------------------
        System.out.println("\n--- 2. Validator & Custom Exceptions (InvalidTransactionException) ---");

        // Test 2a: Reject Bad Amount (negative or zero)
        try {
            System.out.print("Testing rejection of negative amount (-250.00)... ");
            Validator.validateAmount(-250.00);
            System.out.println("FAILED: Should have thrown InvalidTransactionException");
        } catch (InvalidTransactionException e) {
            System.out.println("PASSED: Caught expected exception -> " + e.getMessage());
        }

        // Test 2b: Reject Bad Account Number (non-digits / incorrect length)
        try {
            System.out.print("Testing rejection of bad account ('98765-INV')... ");
            Validator.validateReceiverAccount("98765-INV");
            System.out.println("FAILED: Should have thrown InvalidTransactionException");
        } catch (InvalidTransactionException e) {
            System.out.println("PASSED: Caught expected exception -> " + e.getMessage());
        }

        // Test 2c: Accept valid transaction
        try {
            System.out.print("Testing acceptance of valid transaction... ");
            Transaction validTxn = new DomesticTransaction(1, 2, 4500.00, "987654321001",
                    "Mumbai", "India", "Valid domestic transfer");
            Validator.validateTransaction(validTxn);
            System.out.println("PASSED: Transaction validated successfully!");
        } catch (InvalidTransactionException e) {
            System.out.println("FAILED: Valid transaction was rejected: " + e.getMessage());
        }

        // -----------------------------------------------------------------
        // 3. Generic Result<T> Wrapper Verification
        // -----------------------------------------------------------------
        System.out.println("\n--- 3. Generic Result<T> Demonstration ---");
        Result<String> successResult = Result.ok("TXN-998822", "Transfer processed successfully.");
        Result<String> errorResult = Result.error("Network timeout while contacting payment gateway.");

        System.out.println("Success Result: " + successResult);
        System.out.println("Error Result  : " + errorResult);

        // -----------------------------------------------------------------
        // 4. Polymorphic Models Retest
        // -----------------------------------------------------------------
        System.out.println("\n--- 4. Polymorphic Models Retest ---");
        List<User> users = new ArrayList<>();
        users.add(new Customer(1, "john_doe", "John Doe", "john@example.com", "India"));
        users.add(new Admin(2, "admin_mahi", "Mahi Dhauni", "admin@frauddetect.com", "India"));
        for (User u : users) {
            System.out.printf("User: %-10s | %-8s | %s%n", u.getUsername(), u.getRole(), u.getDashboardTitle());
        }

        System.out.println("\n=========================================================================");
        System.out.println(" Phase 3 Verification Completed Successfully!");
        System.out.println("=========================================================================");
    }
}
