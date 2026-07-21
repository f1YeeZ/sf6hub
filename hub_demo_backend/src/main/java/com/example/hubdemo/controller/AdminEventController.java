package com.example.hubdemo.controller;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class AdminEventController {
    private static final String COMBO_REVIEW_PERMISSION = "combo_review";

    private final CurrentUserService currentUserService;
    private final AdminRealtimeService adminRealtimeService;

    public AdminEventController(CurrentUserService currentUserService, AdminRealtimeService adminRealtimeService) {
        this.currentUserService = currentUserService;
        this.adminRealtimeService = adminRealtimeService;
    }

    @GetMapping(path = "/admin/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(HttpServletRequest request) {
        User user = currentUserService.requireAdminSession(request);
        if (!isAdmin(user) && !hasAdminPermission(user, COMBO_REVIEW_PERMISSION)) {
            throw new BizException("需要连招审核管理权限");
        }
        return adminRealtimeService.subscribeAdmin();
    }

    private static boolean isAdmin(User user) {
        return user != null && "admin".equals(user.getRole());
    }

    private static boolean hasAdminPermission(User user, String permission) {
        if (user == null || !StringUtils.hasText(user.getAdminPermissions())) {
            return false;
        }
        return List.of(user.getAdminPermissions().split(",")).stream()
                .map(String::trim)
                .anyMatch(permission::equals);
    }
}
