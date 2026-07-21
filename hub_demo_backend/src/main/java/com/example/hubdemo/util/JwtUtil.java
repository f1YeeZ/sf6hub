package com.example.hubdemo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtUtil {
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final String EXPECTED_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*\"(\\d+)\"");
    private static final Pattern EXPIRATION_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"ver\"\\s*:\\s*(\\d+)");

    private final String secret;

    public JwtUtil(@Value("${app.jwt-secret}") String secret) {
        this.secret = secret;
    }

    public String createToken(Long userId, String username) {
        return createToken(userId, username, 0L);
    }

    public String createToken(Long userId, String username, Long tokenVersion) {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long exp = Instant.now().plusSeconds(7 * 24 * 3600).getEpochSecond();
        String payload = base64Url("{\"sub\":\"" + userId + "\",\"username\":\"" + escape(username)
                + "\",\"ver\":" + normalizeVersion(tokenVersion) + ",\"exp\":" + exp + "}");
        String unsigned = header + "." + payload;
        return unsigned + "." + sign(unsigned);
    }

    public Optional<Long> parseUserId(String token) {
        return parseClaims(token).map(TokenClaims::userId);
    }

    public Optional<TokenClaims> parseClaims(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            return Optional.empty();
        }
        String normalized = token.startsWith("Bearer ") ? token.substring(7) : token;
        String[] parts = normalized.split("\\.", -1);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            if (!EXPECTED_HEADER.equals(header)) {
                return Optional.empty();
            }
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!MessageDigestHolder.equals(sign(unsigned), parts[2])) {
            return Optional.empty();
        }
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        Matcher expirationMatcher = EXPIRATION_PATTERN.matcher(payload);
        if (!expirationMatcher.find()) {
            return Optional.empty();
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(expirationMatcher.group(1));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        if (expiresAt <= Instant.now().getEpochSecond()) {
            return Optional.empty();
        }
        Matcher subjectMatcher = SUBJECT_PATTERN.matcher(payload);
        if (!subjectMatcher.find()) {
            return Optional.empty();
        }
        try {
            long userId = Long.parseLong(subjectMatcher.group(1));
            Matcher versionMatcher = VERSION_PATTERN.matcher(payload);
            long version = versionMatcher.find() ? Long.parseLong(versionMatcher.group(1)) : 0L;
            return Optional.of(new TokenClaims(userId, version));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static long normalizeVersion(Long version) {
        return version == null ? 0L : Math.max(0L, version);
    }

    public record TokenClaims(Long userId, long tokenVersion) {
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成 JWT", exception);
        }
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class MessageDigestHolder {
        private MessageDigestHolder() {
        }

        static boolean equals(String a, String b) {
            return java.security.MessageDigest.isEqual(
                    a.getBytes(StandardCharsets.UTF_8),
                    b.getBytes(StandardCharsets.UTF_8)
            );
        }
    }
}
