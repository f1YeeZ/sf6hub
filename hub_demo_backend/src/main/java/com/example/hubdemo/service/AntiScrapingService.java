package com.example.hubdemo.service;

import com.example.hubdemo.common.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;

@Service
public class AntiScrapingService {
    private static final Duration COMBO_READ_WINDOW = Duration.ofMinutes(10);
    private static final int ANONYMOUS_COMBO_READ_LIMIT = 80;
    private static final int TRUSTED_COMBO_READ_LIMIT = 240;

    private final RateLimitService rateLimitService;
    private final CsrfTokenService csrfTokenService;

    public AntiScrapingService(RateLimitService rateLimitService, CsrfTokenService csrfTokenService) {
        this.rateLimitService = rateLimitService;
        this.csrfTokenService = csrfTokenService;
    }

    public void checkComboRead(HttpServletRequest request) {
        if (hasBlockedUserAgent(request)) {
            throw blocked();
        }
        boolean trustedBrowserRequest = csrfTokenService.isValid(request);
        rateLimitService.check(
                request,
                trustedBrowserRequest ? "combos:read:trusted" : "combos:read:anonymous",
                trustedBrowserRequest ? TRUSTED_COMBO_READ_LIMIT : ANONYMOUS_COMBO_READ_LIMIT,
                COMBO_READ_WINDOW
        );
        if (!trustedBrowserRequest) {
            throw blocked();
        }
    }

    private static boolean hasBlockedUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) {
            return true;
        }
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        return normalized.contains("python-requests")
                || normalized.contains("scrapy")
                || normalized.contains("httpclient")
                || normalized.contains("go-http-client")
                || normalized.contains("okhttp")
                || normalized.contains("curl")
                || normalized.contains("wget")
                || normalized.contains("bot")
                || normalized.contains("spider")
                || normalized.contains("crawler");
    }

    private static RateLimitException blocked() {
        return new RateLimitException("Combo scraping protection triggered. Please slow down and use the web app.");
    }
}
