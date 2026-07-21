package com.example.hubdemo.service;

import com.example.hubdemo.common.RateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {
    @Test
    void rejectsRequestsAfterLimitIsExceeded() {
        RateLimitService rateLimitService = new RateLimitService(
                Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC)
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        rateLimitService.check(request, "auth:login", 2, Duration.ofMinutes(1));
        rateLimitService.check(request, "auth:login", 2, Duration.ofMinutes(1));

        assertThatThrownBy(() -> rateLimitService.check(request, "auth:login", 2, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitException.class)
                .hasMessage("Too many requests. Please try again later.");
    }

    @Test
    void ignoresForwardedHeadersFromUntrustedClients() {
        RateLimitService rateLimitService = new RateLimitService(
                Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC),
                new ClientIpResolver("")
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.10");
        request.addHeader("X-Forwarded-For", "203.0.113.1");
        rateLimitService.check(request, "auth:login", 1, Duration.ofMinutes(1));

        request.removeHeader("X-Forwarded-For");
        request.addHeader("X-Forwarded-For", "203.0.113.2");
        assertThatThrownBy(() -> rateLimitService.check(request, "auth:login", 1, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitException.class);
    }

    @Test
    void acceptsForwardedAddressOnlyFromConfiguredProxy() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        org.assertj.core.api.Assertions.assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }
}
