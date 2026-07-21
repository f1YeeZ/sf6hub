package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.AnalyticsDtos.VisitTrackRequest;
import com.example.hubdemo.entity.DailyVisitStats;
import com.example.hubdemo.entity.VisitLog;
import com.example.hubdemo.mapper.DailyVisitStatsMapper;
import com.example.hubdemo.mapper.VisitLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class VisitAnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(VisitAnalyticsService.class);
    private static final int DUPLICATE_WINDOW_SECONDS = 8;
    private static final long DASHBOARD_PUSH_INTERVAL_MS = 3000L;

    private final VisitLogMapper visitLogMapper;
    private final DailyVisitStatsMapper dailyVisitStatsMapper;
    private final CurrentUserService currentUserService;
    private final AdminRealtimeService adminRealtimeService;
    private final ClientIpResolver clientIpResolver;
    private final AtomicLong lastDashboardPushAt = new AtomicLong(0);
    private final byte[] ipHashSecret;
    private final int retentionDays;

    @Autowired
    public VisitAnalyticsService(VisitLogMapper visitLogMapper, DailyVisitStatsMapper dailyVisitStatsMapper,
                                  CurrentUserService currentUserService, AdminRealtimeService adminRealtimeService,
                                  ClientIpResolver clientIpResolver,
                                  @Value("${app.analytics-ip-hash-secret:${app.jwt-secret}}") String ipHashSecret,
                                  @Value("${app.analytics-retention-days:90}") int retentionDays) {
        this.visitLogMapper = visitLogMapper;
        this.dailyVisitStatsMapper = dailyVisitStatsMapper;
        this.currentUserService = currentUserService;
        this.adminRealtimeService = adminRealtimeService;
        this.clientIpResolver = clientIpResolver;
        this.ipHashSecret = ipHashSecret.getBytes(StandardCharsets.UTF_8);
        this.retentionDays = Math.max(7, retentionDays);
    }

    VisitAnalyticsService(VisitLogMapper visitLogMapper, DailyVisitStatsMapper dailyVisitStatsMapper,
                          CurrentUserService currentUserService, AdminRealtimeService adminRealtimeService,
                          ClientIpResolver clientIpResolver) {
        this(visitLogMapper, dailyVisitStatsMapper, currentUserService, adminRealtimeService,
                clientIpResolver, "test-only-analytics-secret", 90);
    }

    public Map<String, Object> track(VisitTrackRequest body, HttpServletRequest request) {
        try {
            String visitorId = normalizeVisitorId(body.visitorId());
            String path = normalizePath(body.path());
            LocalDateTime now = LocalDateTime.now();
            long recent = visitLogMapper.countRecentSamePath(visitorId, path, now.minusSeconds(DUPLICATE_WINDOW_SECONDS));
            if (recent > 0) {
                return Map.of("tracked", false, "deduped", true);
            }

            VisitLog log = new VisitLog();
            log.setVisitDate(LocalDate.now());
            log.setVisitorId(visitorId);
            log.setUserId(currentUserService.userIdOrNull(request));
            log.setPath(path);
            log.setReferrer(truncate(stripQueryAndFragment(nullToEmpty(body.referrer()).trim()), 500));
            log.setUserAgent(truncate(nullToEmpty(request.getHeader("User-Agent")).trim(), 500));
            log.setIpHash(hashIp(clientIpResolver.resolve(request)));
            log.setCreatedAt(now);
            visitLogMapper.insert(log);
            publishDashboardRefresh();
            return Map.of("tracked", true, "deduped", false);
        } catch (BizException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Visit analytics tracking unavailable path={}", body.path(), exception);
            return Map.of("tracked", false, "deduped", false, "unavailable", true);
        }
    }

    private void publishDashboardRefresh() {
        long now = System.currentTimeMillis();
        long previous = lastDashboardPushAt.get();
        if (now - previous < DASHBOARD_PUSH_INTERVAL_MS) {
            return;
        }
        if (lastDashboardPushAt.compareAndSet(previous, now)) {
            adminRealtimeService.publishAdmin("visit:track", "dashboard");
        }
    }

    public void refreshDailyStats(LocalDate date) {
        try {
            dailyVisitStatsMapper.upsert(date, visitLogMapper.countUvByDate(date), visitLogMapper.countPvByDate(date));
        } catch (RuntimeException exception) {
            log.warn("Visit analytics refresh skipped for date={}", date, exception);
        }
    }

    @Scheduled(cron = "0 5 * * * *")
    public void refreshRecentDailyStats() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            refreshDailyStats(today.minusDays(i));
        }
    }

    @Scheduled(cron = "${app.analytics-cleanup-cron:0 45 4 * * *}")
    public void deleteExpiredVisitLogs() {
        visitLogMapper.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
    }

    public long uv(LocalDate date) {
        try {
            DailyVisitStats row = dailyVisitStatsMapper.selectById(date);
            return row == null || row.getUv() == null ? 0 : row.getUv();
        } catch (RuntimeException exception) {
            log.warn("Visit analytics UV unavailable for date={}", date, exception);
            return 0;
        }
    }

    public long pv(LocalDate date) {
        try {
            DailyVisitStats row = dailyVisitStatsMapper.selectById(date);
            return row == null || row.getPv() == null ? 0 : row.getPv();
        } catch (RuntimeException exception) {
            log.warn("Visit analytics PV unavailable for date={}", date, exception);
            return 0;
        }
    }

    public List<Map<String, Object>> trend(LocalDate start, LocalDate end) {
        Map<LocalDate, DailyVisitStats> byDate = new LinkedHashMap<>();
        try {
            List<DailyVisitStats> rows = dailyVisitStatsMapper.selectList(new LambdaQueryWrapper<DailyVisitStats>()
                    .ge(DailyVisitStats::getStatDate, start)
                    .le(DailyVisitStats::getStatDate, end));
            rows.forEach(row -> byDate.put(row.getStatDate(), row));
        } catch (RuntimeException exception) {
            log.warn("Visit analytics trend unavailable start={} end={}", start, end, exception);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            DailyVisitStats row = byDate.get(day);
            result.add(Map.of(
                    "date", day.toString(),
                    "uv", row == null || row.getUv() == null ? 0 : row.getUv(),
                    "pv", row == null || row.getPv() == null ? 0 : row.getPv()
            ));
        }
        return result;
    }

    private static String normalizeVisitorId(String value) {
        String trimmed = stripQueryAndFragment(nullToEmpty(value).trim());
        if (!trimmed.matches("[A-Za-z0-9_-]{16,64}")) {
            throw new BizException("访客标识格式不正确");
        }
        return trimmed;
    }

    private static String normalizePath(String value) {
        String trimmed = nullToEmpty(value).trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        return truncate(trimmed, 500);
    }

    private String hashIp(String ip) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(ipHashSecret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(nullToEmpty(ip).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    private static String stripQueryAndFragment(String value) {
        int query = value.indexOf('?');
        int fragment = value.indexOf('#');
        int end = value.length();
        if (query >= 0) end = Math.min(end, query);
        if (fragment >= 0) end = Math.min(end, fragment);
        return value.substring(0, end);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
