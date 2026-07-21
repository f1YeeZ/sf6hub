package com.example.hubdemo.config;

import com.example.hubdemo.service.CsrfTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class CsrfProtectionFilter extends OncePerRequestFilter {
    private final CsrfTokenService csrfTokenService;

    public CsrfProtectionFilter(CsrfTokenService csrfTokenService) {
        this.csrfTokenService = csrfTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        csrfTokenService.ensureToken(request, response);
        if (requiresCsrf(request) && !csrfTokenService.isValid(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"CSRF token invalid\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean requiresCsrf(HttpServletRequest request) {
        return switch (request.getMethod()) {
            case "GET", "HEAD", "OPTIONS" -> false;
            default -> true;
        };
    }
}
