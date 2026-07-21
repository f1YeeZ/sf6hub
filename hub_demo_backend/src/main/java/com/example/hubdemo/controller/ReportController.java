package com.example.hubdemo.controller;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.Report;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.ComboMapper;
import com.example.hubdemo.mapper.ReportMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
public class ReportController {
    private static final List<String> TARGET_TYPES = List.of("combo", "user");
    private static final List<String> REASONS = List.of("spam", "abuse", "duplicate", "wrong_data", "illegal", "other");

    private final CurrentUserService currentUserService;
    private final ReportMapper reportMapper;
    private final ComboMapper comboMapper;
    private final UserMapper userMapper;
    private final RateLimitService rateLimitService;
    private final AdminRealtimeService adminRealtimeService;

    public ReportController(CurrentUserService currentUserService, ReportMapper reportMapper,
                            ComboMapper comboMapper, UserMapper userMapper,
                            RateLimitService rateLimitService, AdminRealtimeService adminRealtimeService) {
        this.currentUserService = currentUserService;
        this.reportMapper = reportMapper;
        this.comboMapper = comboMapper;
        this.userMapper = userMapper;
        this.rateLimitService = rateLimitService;
        this.adminRealtimeService = adminRealtimeService;
    }

    @PostMapping("/reports")
    public Report report(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        rateLimitService.check(request, "reports:create", 10, Duration.ofMinutes(10));
        User currentUser = currentUserService.require(request);
        String targetType = normalizeTargetType(payload.get("targetType"));
        Long targetId = normalizeTargetId(payload.get("targetId"));
        String reason = normalizeReason(payload.get("reason"));
        String detail = normalizeDetail(payload.get("detail"));
        validateTarget(targetType, targetId, currentUser);

        Report report = new Report();
        report.setReporterId(currentUser.getId());
        report.setReporter(currentUser.getUsername());
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDetail(detail);
        report.setStatus("pending");
        report.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);
        adminRealtimeService.publishAdmin("report:create", "reports", "dashboard");
        return report;
    }

    private static String normalizeTargetType(Object value) {
        String targetType = String.valueOf(value == null ? "" : value).trim();
        if (!TARGET_TYPES.contains(targetType)) {
            throw new BizException("举报对象类型不合法");
        }
        return targetType;
    }

    private static Long normalizeTargetId(Object value) {
        try {
            Long targetId = Long.valueOf(String.valueOf(value == null ? "" : value).trim());
            if (targetId <= 0) {
                throw new BizException("举报对象不存在");
            }
            return targetId;
        } catch (NumberFormatException exception) {
            throw new BizException("举报对象不存在");
        }
    }

    private static String normalizeReason(Object value) {
        String reason = String.valueOf(value == null ? "" : value).trim();
        if (!StringUtils.hasText(reason)) {
            throw new BizException("请选择举报理由");
        }
        if (!REASONS.contains(reason)) {
            throw new BizException("举报理由不合法");
        }
        return reason;
    }

    private static String normalizeDetail(Object value) {
        String detail = String.valueOf(value == null ? "" : value).trim();
        if (detail.length() > 500) {
            throw new BizException("补充说明不能超过 500 个字符");
        }
        return detail;
    }

    private void validateTarget(String targetType, Long targetId, User currentUser) {
        if ("combo".equals(targetType)) {
            Combo combo = comboMapper.selectById(targetId);
            if (combo == null) {
                throw new BizException("连招不存在");
            }
            return;
        }
        if ("user".equals(targetType)) {
            User user = userMapper.selectById(targetId);
            if (user == null) {
                throw new BizException("用户不存在");
            }
            if (currentUser.getId() != null && currentUser.getId().equals(user.getId())) {
                throw new BizException("不能举报自己");
            }
            return;
        }
    }
}
