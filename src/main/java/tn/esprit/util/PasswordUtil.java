package tn.esprit.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

/**
 * Utility for hashing and verifying passwords with BCrypt.
 * Never store plain-text passwords in the database.
 */
public final class PasswordUtil {

    private static final int BCRYPT_ROUNDS = 10;
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordUtil() {
    }

    /**
     * Hashes a plain password for storage. Use before persisting to DB.
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) return null;
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifies a plain password against a stored hash. Returns true if they match.
     * Also supports legacy plain-text passwords (backward compatibility during migration).
     */
    public static boolean verify(String plainPassword, String storedValue) {
        if (plainPassword == null || storedValue == null || storedValue.isBlank()) return false;
        if (isGoogleOAuthSentinel(storedValue)) return false;
        if (isHashed(storedValue)) {
            try {
                // ⚠️ Remplacer $2y$ par $2a$ pour compatibilité PHP → Java
                String hashForJava = storedValue.replace("$2y$", "$2a$");
                return BCrypt.checkpw(plainPassword, hashForJava);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return plainPassword.equals(storedValue);
    }

    /**
     * Returns true if the value looks like a BCrypt hash (already hashed).
     */
    public static boolean isHashed(String value) {
        if (value == null || value.length() < 60) return false;
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    /**
     * Returns true if the value is the Google OAuth sentinel (should not be hashed).
     */
    public static boolean isGoogleOAuthSentinel(String value) {
        return "[GOOGLE_OAUTH]".equals(value);
    }

    /**
     * Validates that a password meets strong password requirements.
     * Returns null if valid, or an error message if invalid.
     */
    public static String validateStrong(String password) {
        if (password == null || password.isBlank()) {
            return "Le mot de passe est obligatoire.";
        }
        if (password.length() < MIN_LENGTH) {
            return "Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères.";
        }
        if (!UPPERCASE.matcher(password).find()) {
            return "Le mot de passe doit contenir au moins une lettre majuscule.";
        }
        if (!LOWERCASE.matcher(password).find()) {
            return "Le mot de passe doit contenir au moins une lettre minuscule.";
        }
        if (!DIGIT.matcher(password).find()) {
            return "Le mot de passe doit contenir au moins un chiffre.";
        }
        if (!SPECIAL.matcher(password).find()) {
            return "Le mot de passe doit contenir au moins un caractère spécial (!@#$%^&*...).";
        }
        return null;
    }

    /** Returns the hint text for strong password requirements. */
    public static String getStrongPasswordHint() {
        return "Min. 8 caractères, majuscule, minuscule, chiffre et caractère spécial";
    }

    /** Individual checks for real-time password strength UI. */
    public static boolean hasMinLength(String password) {
        return password != null && password.length() >= MIN_LENGTH;
    }
    public static boolean hasUppercase(String password) {
        return password != null && UPPERCASE.matcher(password).find();
    }
    public static boolean hasLowercase(String password) {
        return password != null && LOWERCASE.matcher(password).find();
    }
    public static boolean hasDigit(String password) {
        return password != null && DIGIT.matcher(password).find();
    }
    public static boolean hasSpecialChar(String password) {
        return password != null && SPECIAL.matcher(password).find();
    }
}
