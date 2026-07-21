package com.example.hubdemo.util;

import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

    private PasswordUtil() {
    }

    public static String hash(String password) {
        return BCRYPT.encode(password);
    }

    public static boolean matches(String password, String stored) {
        if (stored == null || stored.isBlank()) {
            return false;
        }
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return BCRYPT.matches(password, stored);
        }
        return matchesLegacySha256(password, stored);
    }

    private static boolean matchesLegacySha256(String password, String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            byte[] salt = HexFormat.of().parseHex(parts[0]);
            return MessageDigest.isEqual(HexFormat.of().parseHex(parts[1]), sha256(salt, password));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] sha256(byte[] salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
