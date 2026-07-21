package com.example.hubdemo.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthCookieService {
    private static final String COOKIE_NAME = "hub_auth";
    private static final String ADMIN_COOKIE_NAME = "hub_admin_auth";
    private static final int MAX_AGE_SECONDS = 7 * 24 * 3600;

    private final boolean secureCookie;

    public AuthCookieService(@Value("${app.public-base-url}") String publicBaseUrl) {
        this.secureCookie = publicBaseUrl != null && publicBaseUrl.startsWith("https://");
    }

    public void setAuthCookie(HttpServletResponse response, String token) {
        setCookie(response, COOKIE_NAME, token);
    }

    public void setAdminAuthCookie(HttpServletResponse response, String token) {
        setCookie(response, ADMIN_COOKIE_NAME, token);
    }

    public void clearAuthCookie(HttpServletResponse response) {
        clearCookie(response, COOKIE_NAME);
    }

    public void clearAdminAuthCookie(HttpServletResponse response) {
        clearCookie(response, ADMIN_COOKIE_NAME);
    }

    public String readAuthCookie(HttpServletRequest request) {
        return readCookie(request, COOKIE_NAME);
    }

    public String readAdminAuthCookie(HttpServletRequest request) {
        return readCookie(request, ADMIN_COOKIE_NAME);
    }

    private void setCookie(HttpServletResponse response, String name, String token) {
        ResponseCookie cookie = ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api")
                .maxAge(MAX_AGE_SECONDS)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return "";
    }
}
