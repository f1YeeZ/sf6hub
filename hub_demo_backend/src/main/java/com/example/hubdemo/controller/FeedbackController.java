package com.example.hubdemo.controller;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.FeedbackDtos.FeedbackRequest;
import com.example.hubdemo.entity.Feedback;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.FeedbackMapper;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
public class FeedbackController {
    private final CurrentUserService currentUserService;
    private final FeedbackMapper feedbackMapper;
    private final RateLimitService rateLimitService;
    private final AdminRealtimeService adminRealtimeService;

    public FeedbackController(CurrentUserService currentUserService, FeedbackMapper feedbackMapper,
                              RateLimitService rateLimitService, AdminRealtimeService adminRealtimeService) {
        this.currentUserService = currentUserService;
        this.feedbackMapper = feedbackMapper;
        this.rateLimitService = rateLimitService;
        this.adminRealtimeService = adminRealtimeService;
    }

    @PostMapping("/feedback")
    public Feedback submitFeedback(@Valid @RequestBody FeedbackRequest feedback, HttpServletRequest request) {
        rateLimitService.check(request, "feedback:create", 10, Duration.ofMinutes(10));
        User currentUser = currentUserService.require(request);
        String detail = normalizeDetail(feedback.detail());

        Feedback saved = new Feedback();
        saved.setUserId(currentUser.getId());
        saved.setUsername(currentUser.getUsername());
        saved.setReason(feedback.reason().trim());
        saved.setDetail(detail);
        saved.setStatus("pending");
        saved.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(saved);
        adminRealtimeService.publishAdmin("feedback:create", "feedbacks", "dashboard");
        return saved;
    }

    private static String normalizeDetail(String value) {
        String detail = value == null ? "" : value.trim();
        if (!StringUtils.hasText(detail)) {
            throw new BizException("反馈内容不能为空");
        }
        if (detail.length() > 500) {
            throw new BizException("反馈内容不能超过 500 个字符");
        }
        return detail;
    }
}
