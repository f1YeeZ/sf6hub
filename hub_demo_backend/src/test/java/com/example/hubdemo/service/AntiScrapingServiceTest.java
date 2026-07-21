package com.example.hubdemo.service;

import com.example.hubdemo.common.RateLimitException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AntiScrapingServiceTest {
    @Test
    void rejectsComboReadsWithoutUserAgent() {
        AntiScrapingService service = newService();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/combos/1");
        request.setRemoteAddr("127.0.0.1");
        withValidCsrf(request);

        assertThatThrownBy(() -> service.checkComboRead(request))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Combo scraping protection triggered");
    }

    @Test
    void rejectsComboReadsWithoutCsrfProof() {
        AntiScrapingService service = newService();
        MockHttpServletRequest request = browserApiRequest();

        assertThatThrownBy(() -> service.checkComboRead(request))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Combo scraping protection triggered");
    }

    @Test
    void allowsComboReadsWithBrowserUserAgentAndCsrfProof() {
        AntiScrapingService service = newService();
        MockHttpServletRequest request = browserApiRequest();
        withValidCsrf(request);

        service.checkComboRead(request);
    }

    @Test
    void rateLimitsRepeatedTrustedComboReads() {
        AntiScrapingService service = newService();
        MockHttpServletRequest request = browserApiRequest();
        withValidCsrf(request);

        for (int i = 0; i < 240; i++) {
            service.checkComboRead(request);
        }

        assertThatThrownBy(() -> service.checkComboRead(request))
                .isInstanceOf(RateLimitException.class)
                .hasMessage("Too many requests. Please try again later.");
    }

    private static AntiScrapingService newService() {
        RateLimitService rateLimitService = new RateLimitService(
                Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC)
        );
        return new AntiScrapingService(rateLimitService, new CsrfTokenService(""));
    }

    private static MockHttpServletRequest browserApiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/combos/1");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36");
        request.addHeader("Sec-Fetch-Mode", "cors");
        return request;
    }

    private static void withValidCsrf(MockHttpServletRequest request) {
        request.setCookies(new Cookie(CsrfTokenService.COOKIE_NAME, "test-token"));
        request.addHeader(CsrfTokenService.HEADER_NAME, "test-token");
    }
}
