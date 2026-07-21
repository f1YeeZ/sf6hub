package com.example.hubdemo.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {
    private static final String SECRET = "test-secret-with-enough-entropy";

    @Test
    void parseUserIdRejectsExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        String token = tokenWithPayload("{\"sub\":\"42\",\"username\":\"player\",\"exp\":"
                + Instant.now().minusSeconds(60).getEpochSecond() + "}");

        assertThat(jwtUtil.parseUserId(token)).isEmpty();
    }

    @Test
    void parseUserIdRejectsTokenWithoutExpiration() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        String token = tokenWithPayload("{\"sub\":\"42\",\"username\":\"player\"}");

        assertThat(jwtUtil.parseUserId(token)).isEmpty();
    }

    @Test
    void parseUserIdAcceptsValidToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        String token = jwtUtil.createToken(42L, "player");

        assertThat(jwtUtil.parseUserId("Bearer " + token)).contains(42L);
    }

    @Test
    void tokenCarriesSessionVersion() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        String token = jwtUtil.createToken(42L, "player", 7L);

        assertThat(jwtUtil.parseClaims(token)).contains(new JwtUtil.TokenClaims(42L, 7L));
    }

    private static String tokenWithPayload(String payload) {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String body = base64Url(payload);
        String unsigned = header + "." + body;
        return unsigned + "." + sign(unsigned);
    }

    private static String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
