package com.eauction.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing and verification using BCrypt (cost factor 12).
 *
 * BCrypt automatically embeds a random salt into the hash, so the same
 * plaintext produces a different hash on every call – which is exactly what
 * we want.  Old SHA-256 hashes stored in the database will fail verification
 * after migration; a one-time migration script should re-hash all passwords.
 */
public final class PasswordUtil {

    /** BCrypt work-factor.  Increase to 13-14 on production hardware. */
    private static final int BCRYPT_ROUNDS = 12;

    private PasswordUtil() { /* utility class */ }

    /**
     * Hashes a plaintext password with BCrypt.
     *
     * @param plaintext raw password – must not be null or empty
     * @return BCrypt hash string (60 chars)
     */
    public static String hashPassword(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifies a plaintext password against a BCrypt hash.
     *
     * @param plaintext    the password the user just typed
     * @param storedHash   the hash retrieved from the database
     * @return true if they match
     */
    public static boolean verifyPassword(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null) return false;
        try {
            return BCrypt.checkpw(plaintext, storedHash);
        } catch (IllegalArgumentException e) {
            // Stored hash is not a valid BCrypt string (e.g. old SHA-256 value)
            return false;
        }
    }

    /**
     * Quick helper to validate minimum password policy (≥ 8 chars,
     * at least one digit, at least one letter).
     */
    public static boolean meetsPolicy(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c))  hasLetter = true;
            if (Character.isDigit(c))   hasDigit  = true;
        }
        return hasLetter && hasDigit;
    }
}
