package com.example.hubdemo.controller;

import com.example.hubdemo.dto.AnalyticsDtos.VisitTrackRequest;
import com.example.hubdemo.service.RateLimitService;
import com.example.hubdemo.service.VisitAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsControllerTest {
    @Test
    void rateLimitsVisitTrackingBeforeWritingAnalytics() {
        VisitAnalyticsService visitAnalyticsService = mock(VisitAnalyticsService.class);
        RateLimitService rateLimitService = mock(RateLimitService.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        VisitTrackRequest body = new VisitTrackRequest("/", "0123456789abcdef", "");
        Map<String, Object> response = Map.of("tracked", true, "deduped", false);
        when(visitAnalyticsService.track(body, request)).thenReturn(response);

        AnalyticsController controller = new AnalyticsController(visitAnalyticsService, rateLimitService);

        assertThat(controller.trackVisit(body, request)).isSameAs(response);
        verify(rateLimitService).check(eq(request), eq("analytics:visit"), eq(300), any());
        verify(visitAnalyticsService).track(body, request);
    }
}
