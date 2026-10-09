package com.frauddetect.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility class providing cryptographic hashing and verification for user passwords.
 * Implements salted SHA-256 hashing and timing-attack resistant verification.
 */
public final class PasswordUtil {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_BYTE_LENGTH = 16; // 16 bytes -> 32 hex chars
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Generates a cryptographically strong pseudo-random salt using SecureRandom.
     *
     * @return 32-character hexadecimal salt string
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[SALT_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);
        return bytesToHex(saltBytes);
    }

    /**
     * Computes the SHA-256 hash of a password concatenated with a salt.
     *
     * @param password the plain-text password
     * @param salt the random salt
     * @return 64-character hexadecimal SHA-256 digest
     */
    public static String hash(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            // Combine salt and password bytes
            String saltedInput = salt + password;
            byte[] hashBytes = digest.digest(saltedInput.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JRE", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored salt and expected hash.
     * Uses MessageDigest.isEqual for constant-time byte comparison to protect against timing attacks.
     *
     * @param password the plain-text password to verify
     * @param salt the salt stored for the user
     * @param expectedHash the hash stored for the user
     * @return true if matches, false otherwise
     */
    public static boolean verify(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        String computedHash = hash(password, salt);
        byte[] computedBytes = computedHash.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expectedHash.getBytes(StandardCharsets.UTF_8);

        // Constant-time comparison to prevent side-channel timing analysis
        return MessageDigest.isEqual(computedBytes, expectedBytes);
    }

    /**
     * Helper to convert raw bytes into a lower-case hexadecimal string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
