package com.example.hubdemo.controller;

import com.example.hubdemo.common.PageResult;
import com.example.hubdemo.entity.Notification;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.NotificationService;
import com.example.hubdemo.service.RateLimitService;
import com.example.hubdemo.util.PageUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final RateLimitService rateLimitService;
    private final AdminRealtimeService adminRealtimeService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService,
                                  RateLimitService rateLimitService, AdminRealtimeService adminRealtimeService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
        this.rateLimitService = rateLimitService;
        this.adminRealtimeService = adminRealtimeService;
    }

    @GetMapping("/notifications")
    public PageResult<Notification> notifications(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long pageSize,
                                                  @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                  @RequestParam(defaultValue = "") String type,
                                                  HttpServletRequest servletRequest) {
        User currentUser = currentUserService.require(servletRequest);
        return notificationService.list(currentUser.getId(), PageUtil.page(page), PageUtil.pageSize(pageSize), unreadOnly, type);
    }

    @PostMapping("/notifications/{id}/read")
    public Notification read(@PathVariable Long id, HttpServletRequest servletRequest) {
        rateLimitService.check(servletRequest, "notifications:read", 240, Duration.ofMinutes(10));
        User currentUser = currentUserService.require(servletRequest);
        Notification notification = notificationService.read(id, currentUser.getId());
        adminRealtimeService.publishPublic("notification:read", "notifications");
        return notification;
    }

    @PostMapping("/notifications/read-all")
    public Map<String, Integer> readAll(@RequestParam(defaultValue = "") String type,
                                        HttpServletRequest servletRequest) {
        rateLimitService.check(servletRequest, "notifications:read-all", 60, Duration.ofMinutes(10));
        User currentUser = currentUserService.require(servletRequest);
        int count = notificationService.readAll(currentUser.getId(), type);
        if (count > 0) {
            adminRealtimeService.publishPublic("notification:read-all", "notifications");
        }
        return Map.of("count", count);
    }

}
