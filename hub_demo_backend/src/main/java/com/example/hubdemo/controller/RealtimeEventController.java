package com.example.hubdemo.controller;

import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.ClientIpResolver;
import com.example.hubdemo.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class RealtimeEventController {
    private final AdminRealtimeService adminRealtimeService;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitService rateLimitService;

    public RealtimeEventController(AdminRealtimeService adminRealtimeService, ClientIpResolver clientIpResolver,
                                   RateLimitService rateLimitService) {
        this.adminRealtimeService = adminRealtimeService;
        this.clientIpResolver = clientIpResolver;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(HttpServletRequest request) {
        rateLimitService.check(request, "realtime:connect", 30, java.time.Duration.ofMinutes(10));
        return adminRealtimeService.subscribePublic(clientIpResolver.resolve(request));
    }
}
