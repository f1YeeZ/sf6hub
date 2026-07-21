package com.example.hubdemo.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CsrfTokenService {
    public static final String COOKIE_NAME = "hub_csrf";
    public static final String HEADER_NAME = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_AGE_SECONDS = 7 * 24 * 3600;

    private final boolean secureCookie;

    public CsrfTokenService(@Value("${app.public-base-url}") String publicBaseUrl) {
        this.secureCookie = publicBaseUrl != null && publicBaseUrl.startsWith("https://");
    }

    public String ensureToken(HttpServletRequest request, HttpServletResponse response) {
        String token = readCookie(request);
        if (StringUtils.hasText(token)) {
            response.setHeader(HEADER_NAME, token);
            return token;
        }
        token = createToken();
        setToken(response, token, MAX_AGE_SECONDS);
        response.setHeader(HEADER_NAME, token);
        return token;
    }

    public boolean isValid(HttpServletRequest request) {
        String cookieToken = readCookie(request);
        String headerToken = request.getHeader(HEADER_NAME);
        return StringUtils.hasText(cookieToken)
                && StringUtils.hasText(headerToken)
                && java.security.MessageDigest.isEqual(cookieToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                headerToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public void rotate(HttpServletResponse response) {
        String token = createToken();
        setToken(response, token, MAX_AGE_SECONDS);
        response.setHeader(HEADER_NAME, token);
    }

    public void clear(HttpServletResponse response) {
        setToken(response, "", 0);
        response.setHeader(HEADER_NAME, "");
    }

    private void setToken(HttpServletResponse response, String token, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(false)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private static String createToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "";
    }
}
