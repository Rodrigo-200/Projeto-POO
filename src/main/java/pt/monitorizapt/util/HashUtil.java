package pt.monitorizapt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for cryptographic operations.
 * Used to generate SHA-256 signatures for sensor data validation.
 */
public final class HashUtil {
    // Fast lookup table for Hex conversion
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private HashUtil() {
    }

    /**
     * Generates a SHA-256 hash from a string input.
     * SHA-256 is used because it is a standard, secure one-way hashing algorithm.
     *
     * @param input The string to hash.
     * @return The hexadecimal representation of the hash.
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // This should never happen in a standard Java environment
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Converts a byte array into a Hexadecimal string.
     * We do this manually to avoid adding heavy external dependencies (like Apache Commons)
     * just for this simple task.
     */
    private static String toHex(byte[] data) {
        char[] chars = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            chars[i * 2] = HEX_ARRAY[v >>> 4];
            chars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(chars);
    }
}