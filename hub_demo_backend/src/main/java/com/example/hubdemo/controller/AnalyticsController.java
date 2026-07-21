package com.example.hubdemo.controller;

import com.example.hubdemo.dto.AnalyticsDtos.VisitTrackRequest;
import com.example.hubdemo.service.RateLimitService;
import com.example.hubdemo.service.VisitAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
public class AnalyticsController {
    private final VisitAnalyticsService visitAnalyticsService;
    private final RateLimitService rateLimitService;

    public AnalyticsController(VisitAnalyticsService visitAnalyticsService, RateLimitService rateLimitService) {
        this.visitAnalyticsService = visitAnalyticsService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/analytics/visit")
    public Map<String, Object> trackVisit(@Valid @RequestBody VisitTrackRequest body, HttpServletRequest request) {
        rateLimitService.check(request, "analytics:visit", 300, Duration.ofMinutes(10));
        return visitAnalyticsService.track(body, request);
    }
}
