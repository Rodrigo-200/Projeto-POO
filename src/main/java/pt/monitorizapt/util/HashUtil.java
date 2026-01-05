package pt.monitorizapt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private HashUtil() {
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

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
