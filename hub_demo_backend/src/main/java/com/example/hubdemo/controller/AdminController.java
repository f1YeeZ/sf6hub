package com.example.hubdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.common.PageResult;
import com.example.hubdemo.dto.AuthDtos.AuthResponse;
import com.example.hubdemo.dto.AdminDtos.AnnouncementRequest;
import com.example.hubdemo.dto.AdminDtos.CharacterOrderRequest;
import com.example.hubdemo.dto.AdminDtos.CharacterRequest;
import com.example.hubdemo.dto.AdminDtos.ComboReviewRequest;
import com.example.hubdemo.dto.AdminDtos.ComboVideoReviewRequest;
import com.example.hubdemo.dto.AdminDtos.FrameRequest;
import com.example.hubdemo.dto.AdminDtos.NotificationBroadcastRequest;
import com.example.hubdemo.dto.AdminDtos.ReportActionRequest;
import com.example.hubdemo.dto.AdminDtos.ReportAdminResponse;
import com.example.hubdemo.dto.AdminDtos.ReportBatchActionRequest;
import com.example.hubdemo.dto.AdminDtos.UserAdminRequest;
import com.example.hubdemo.dto.ComboDtos.ComboRequest;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCheckResponse;
import com.example.hubdemo.dto.FeedbackDtos.FeedbackActionRequest;
import com.example.hubdemo.dto.FeedbackDtos.FeedbackAdminResponse;
import com.example.hubdemo.dto.FeedbackDtos.FeedbackBatchActionRequest;
import com.example.hubdemo.entity.AdminAuditLog;
import com.example.hubdemo.entity.Announcement;
import com.example.hubdemo.entity.CharacterInfo;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.Feedback;
import com.example.hubdemo.entity.FrameChangeHistory;
import com.example.hubdemo.entity.FrameData;
import com.example.hubdemo.entity.FrameSyncLog;
import com.example.hubdemo.entity.Report;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.AdminAuditLogMapper;
import com.example.hubdemo.mapper.AdminDashboardMapper;
import com.example.hubdemo.mapper.AnnouncementMapper;
import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.ComboMapper;
import com.example.hubdemo.mapper.FeedbackMapper;
import com.example.hubdemo.mapper.FrameChangeHistoryMapper;
import com.example.hubdemo.mapper.FrameDataMapper;
import com.example.hubdemo.mapper.FrameSyncLogMapper;
import com.example.hubdemo.mapper.ReportMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.service.AdminAuditService;
import com.example.hubdemo.service.AdminRealtimeService;
import com.example.hubdemo.service.CatalogService;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.FrameSyncLogService;
import com.example.hubdemo.service.NotificationService;
import com.example.hubdemo.service.OfficialFrameSyncService;
import com.example.hubdemo.service.UserService;
import com.example.hubdemo.service.VisitAnalyticsService;
import com.example.hubdemo.util.PageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class AdminController {
    private static final String COMBO_REVIEW_PERMISSION = "combo_review";

    private final CatalogService catalogService;
    private final UserService userService;
    private final OfficialFrameSyncService officialFrameSyncService;
    private final FrameSyncLogService frameSyncLogService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final AdminAuditService adminAuditService;
    private final AdminRealtimeService adminRealtimeService;
    private final UserMapper userMapper;
    private final CharacterMapper characterMapper;
    private final FrameDataMapper frameDataMapper;
    private final FrameSyncLogMapper frameSyncLogMapper;
    private final FrameChangeHistoryMapper frameChangeHistoryMapper;
    private final ComboMapper comboMapper;
    private final ReportMapper reportMapper;
    private final FeedbackMapper feedbackMapper;
    private final AnnouncementMapper announcementMapper;
    private final AdminAuditLogMapper adminAuditLogMapper;
    private final VisitAnalyticsService visitAnalyticsService;
    private final AdminDashboardMapper adminDashboardMapper;
    public AdminController(CatalogService catalogService, UserService userService,
                           OfficialFrameSyncService officialFrameSyncService,
                           FrameSyncLogService frameSyncLogService,
                           CurrentUserService currentUserService, NotificationService notificationService,
                           AdminAuditService adminAuditService, AdminRealtimeService adminRealtimeService, UserMapper userMapper,
                           CharacterMapper characterMapper, FrameDataMapper frameDataMapper,
                           FrameSyncLogMapper frameSyncLogMapper, FrameChangeHistoryMapper frameChangeHistoryMapper,
                           ComboMapper comboMapper, ReportMapper reportMapper, FeedbackMapper feedbackMapper, AnnouncementMapper announcementMapper,
                           AdminAuditLogMapper adminAuditLogMapper, VisitAnalyticsService visitAnalyticsService,
                           AdminDashboardMapper adminDashboardMapper) {
        this.catalogService = catalogService;
        this.userService = userService;
        this.officialFrameSyncService = officialFrameSyncService;
        this.frameSyncLogService = frameSyncLogService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.adminAuditService = adminAuditService;
        this.adminRealtimeService = adminRealtimeService;
        this.userMapper = userMapper;
        this.characterMapper = characterMapper;
        this.frameDataMapper = frameDataMapper;
        this.frameSyncLogMapper = frameSyncLogMapper;
        this.frameChangeHistoryMapper = frameChangeHistoryMapper;
        this.comboMapper = comboMapper;
        this.reportMapper = reportMapper;
        this.feedbackMapper = feedbackMapper;
        this.announcementMapper = announcementMapper;
        this.adminAuditLogMapper = adminAuditLogMapper;
        this.visitAnalyticsService = visitAnalyticsService;
        this.adminDashboardMapper = adminDashboardMapper;
    }

    @GetMapping("/admin/me")
    public AuthResponse me(HttpServletRequest request) {
        User user = requireComboManager(request);
        user.setPasswordHash(null);
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                "",
                user.getRole(),
                user.getAvatar(),
                user.getAdminPermissions()
        );
    }

    @GetMapping("/admin/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest request) {
        requireAdmin(request);
        LocalDate today = LocalDate.now();
        Map<String, Object> counts = adminDashboardMapper.counts(today, today.atStartOfDay());
        long pendingCombos = count(counts, "pendingCombos");
        long manualReviewCombos = count(counts, "manualReviewCombos");
        long pendingReports = count(counts, "pendingReports");
        long pendingFeedbacks = count(counts, "pendingFeedbacks");
        Map<String, Object> result = new LinkedHashMap<>();
        counts.forEach(result::put);
        result.put("reviewQueueCombos", pendingCombos + manualReviewCombos);
        result.put("todayUv", visitAnalyticsService.uv(today));
        result.put("todayPv", visitAnalyticsService.pv(today));
        result.put("userTrend", dailyUserTrend(today.minusDays(29), today));
        result.put("comboTrend", dailyComboTrend(today.minusDays(29), today));
        result.put("visitTrend", visitAnalyticsService.trend(today.minusDays(29), today));
        result.put("popularCombos", popularCombos());
        result.put("todayTodos", List.of(
                Map.of("key", "manual_review", "label", "待处理投稿", "count", pendingCombos + manualReviewCombos),
                Map.of("key", "reports", "label", "待处理举报", "count", pendingReports),
                Map.of("key", "feedbacks", "label", "待处理反馈", "count", pendingFeedbacks)
        ));
        List<Combo> riskCombos = comboMapper.selectRiskCombos(16);
        Map<Long, String> riskCharacterNames = characterNames(riskCombos);
        result.put("highRiskCombos", highRiskCombos(riskCombos, riskCharacterNames));
        result.put("issueReviewCombos", issueReviewCombos(riskCombos, riskCharacterNames));
        result.put("recentAuditLogs", recentAuditLogs());
        return result;
    }

    private static long count(Map<String, Object> counts, String key) {
        Object value = counts.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private List<Map<String, Object>> dailyUserTrend(LocalDate start, LocalDate end) {
        Map<LocalDate, Long> counts = dailyCounts(userMapper.countCreatedByDay(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            rows.add(Map.of("date", day.toString(), "value", counts.getOrDefault(day, 0L)));
        }
        return rows;
    }

    private List<Map<String, Object>> dailyComboTrend(LocalDate start, LocalDate end) {
        Map<LocalDate, Long> counts = dailyCounts(comboMapper.countCreatedByDay(start, end));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            rows.add(Map.of("date", day.toString(), "value", counts.getOrDefault(day, 0L)));
        }
        return rows;
    }

    private List<Map<String, Object>> popularCombos() {
        List<Combo> combos = comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                        .orderByDesc(Combo::getFavorites)
                        .orderByDesc(Combo::getLikes)
                        .orderByDesc(Combo::getId)
                        .last("limit 10"));
        Map<Long, String> characterNames = characterNames(combos);
        return combos.stream()
                .map(combo -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", combo.getId());
                    row.put("name", StringUtils.hasText(combo.getRoute()) ? combo.getRoute() : combo.getStarter());
                    row.put("character", characterNames.getOrDefault(combo.getCharacterId(), "#" + combo.getCharacterId()));
                    row.put("favorites", combo.getFavorites() == null ? 0 : combo.getFavorites());
                    row.put("likes", combo.getLikes() == null ? 0 : combo.getLikes());
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> highRiskCombos(List<Combo> combos, Map<Long, String> characterNames) {
        return combos.stream()
                .filter(combo -> scoreComboRisk(combo) > 0)
                .sorted((left, right) -> Integer.compare(scoreComboRisk(right), scoreComboRisk(left)))
                .limit(8)
                .map(combo -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", combo.getId());
                    row.put("character", characterNames.getOrDefault(combo.getCharacterId(), "#" + combo.getCharacterId()));
                    row.put("starter", combo.getStarter());
                    row.put("route", combo.getRoute());
                    row.put("status", combo.getStatus());
                    row.put("riskScore", scoreComboRisk(combo));
                    row.put("reportCount", combo.getReportCount() == null ? 0 : combo.getReportCount());
                    row.put("manualReviewReason", nullToEmpty(combo.getManualReviewReason()));
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> issueReviewCombos(List<Combo> combos, Map<Long, String> characterNames) {
        return combos.stream()
                .filter(combo -> "manual_review".equals(combo.getStatus()) || scoreComboRisk(combo) >= 40)
                .sorted((left, right) -> Integer.compare(scoreComboRisk(right), scoreComboRisk(left)))
                .limit(8)
                .map(combo -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", combo.getId());
                    row.put("character", characterNames.getOrDefault(combo.getCharacterId(), "#" + combo.getCharacterId()));
                    row.put("starter", combo.getStarter());
                    row.put("route", combo.getRoute());
                    row.put("status", combo.getStatus());
                    row.put("riskScore", scoreComboRisk(combo));
                    row.put("reportCount", combo.getReportCount() == null ? 0 : combo.getReportCount());
                    row.put("manualReviewReason", nullToEmpty(combo.getManualReviewReason()));
                    return row;
                })
                .toList();
    }

    private List<AdminAuditLog> recentAuditLogs() {
        return adminAuditLogMapper.selectList(new LambdaQueryWrapper<AdminAuditLog>()
                .orderByDesc(AdminAuditLog::getCreatedAt)
                .last("limit 8"));
    }

    private Map<Long, String> characterNames(List<Combo> combos) {
        List<Long> ids = combos.stream().map(Combo::getCharacterId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return characterMapper.selectList(new LambdaQueryWrapper<CharacterInfo>()
                        .select(CharacterInfo::getId, CharacterInfo::getName)
                        .in(CharacterInfo::getId, ids))
                .stream().collect(Collectors.toMap(CharacterInfo::getId, CharacterInfo::getName));
    }

    private static Map<LocalDate, Long> dailyCounts(List<Map<String, Object>> rows) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object rawDay = row.get("day");
            Object rawValue = row.get("value");
            if (rawDay != null && rawValue instanceof Number number) {
                result.put(LocalDate.parse(rawDay.toString()), number.longValue());
            }
        }
        return result;
    }

    private void applyComboRiskSignals(List<Combo> combos) {
        if (combos == null || combos.isEmpty()) {
            return;
        }
        List<Long> comboIds = combos.stream().map(Combo::getId).filter(Objects::nonNull).toList();
        Map<Long, Long> reportsByCombo = reportMapper.selectList(new LambdaQueryWrapper<Report>()
                        .select(Report::getTargetId)
                        .eq(Report::getTargetType, "combo")
                        .eq(Report::getStatus, "pending")
                        .in(!comboIds.isEmpty(), Report::getTargetId, comboIds))
                .stream().collect(Collectors.groupingBy(Report::getTargetId, Collectors.counting()));
        combos.forEach(combo -> {
            combo.setReportCount(reportsByCombo.getOrDefault(combo.getId(), 0L));
            combo.setReviewPriority(scoreComboRisk(combo));
        });
    }

    private int scoreComboRisk(Combo combo) {
        if (combo == null) {
            return 0;
        }
        int score = 0;
        if ("manual_review".equals(combo.getStatus())) {
            score += 60;
        }
        score += (int) Math.min(40, (combo.getReportCount() == null ? 0 : combo.getReportCount()) * 12);
        if (!StringUtils.hasText(combo.getVideoUrl())) {
            score += 12;
        }
        if ("video_rejected".equals(combo.getVideoReviewStatus())) {
            score += 80;
        }
        if ("pending".equals(combo.getStatus())) {
            score += 10;
        }
        return score;
    }

    private void applyUserGovernanceStats(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(User::getId).filter(Objects::nonNull).toList();
        List<Combo> combos = comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                .select(Combo::getId, Combo::getAuthorId)
                .in(Combo::getAuthorId, userIds));
        Map<Long, Long> comboCounts = combos.stream()
                .collect(Collectors.groupingBy(Combo::getAuthorId, Collectors.counting()));
        Map<Long, Long> authorByCombo = combos.stream()
                .collect(Collectors.toMap(Combo::getId, Combo::getAuthorId));
        Map<Long, Long> reportCounts = authorByCombo.isEmpty() ? Map.of()
                : reportMapper.selectList(new LambdaQueryWrapper<Report>()
                        .select(Report::getTargetId)
                        .eq(Report::getTargetType, "combo")
                        .in(Report::getTargetId, authorByCombo.keySet()))
                .stream()
                .map(report -> authorByCombo.get(report.getTargetId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        users.forEach(user -> {
            user.setComboCount(comboCounts.getOrDefault(user.getId(), 0L));
            user.setReportCount(reportCounts.getOrDefault(user.getId(), 0L));
        });
    }

    private void applyCharacterContentStats(List<CharacterInfo> characters) {
        if (characters == null || characters.isEmpty()) {
            return;
        }
        List<Long> ids = characters.stream().map(CharacterInfo::getId).filter(Objects::nonNull).toList();
        List<Combo> combos = comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                .select(Combo::getCharacterId, Combo::getStatus)
                .in(Combo::getCharacterId, ids));
        List<FrameData> frames = frameDataMapper.selectList(new LambdaQueryWrapper<FrameData>()
                .select(FrameData::getCharacterId)
                .in(FrameData::getCharacterId, ids));
        Map<Long, Long> approved = combos.stream().filter(combo -> "approved".equals(combo.getStatus()))
                .collect(Collectors.groupingBy(Combo::getCharacterId, Collectors.counting()));
        Map<Long, Long> pending = combos.stream().filter(combo -> List.of("pending", "manual_review").contains(combo.getStatus()))
                .collect(Collectors.groupingBy(Combo::getCharacterId, Collectors.counting()));
        Map<Long, Long> frameCounts = frames.stream()
                .collect(Collectors.groupingBy(FrameData::getCharacterId, Collectors.counting()));
        characters.forEach(character -> {
            character.setComboCount(approved.getOrDefault(character.getId(), 0L));
            character.setPendingComboCount(pending.getOrDefault(character.getId(), 0L));
            character.setFrameCount(frameCounts.getOrDefault(character.getId(), 0L));
        });
    }

    private void applyFrameMaintenanceStats(List<FrameData> frames) {
        if (frames == null || frames.isEmpty()) {
            return;
        }
        LocalDateTime recentCutoff = LocalDateTime.now().minusDays(7);
        List<Long> frameIds = frames.stream().map(FrameData::getId).filter(Objects::nonNull).toList();
        Set<Long> recentlyChanged = frameIds.isEmpty() ? Set.of()
                : frameChangeHistoryMapper.selectList(new LambdaQueryWrapper<FrameChangeHistory>()
                        .select(FrameChangeHistory::getFrameId)
                        .in(FrameChangeHistory::getFrameId, frameIds)
                        .ge(FrameChangeHistory::getCreatedAt, recentCutoff))
                .stream().map(FrameChangeHistory::getFrameId).collect(Collectors.toSet());
        frames.forEach(frame -> {
            frame.setRecentlyChanged(recentlyChanged.contains(frame.getId()));
            frame.setManuallyCorrected(StringUtils.hasText(frame.getSourceUrl()) && frame.getSourceUrl().startsWith("manual:"));
        });
    }

    private void applyComboParentSummaries(List<Combo> combos) {
        if (combos == null || combos.isEmpty()) {
            return;
        }
        List<Long> parentIds = combos.stream().map(Combo::getFollowupParentId).filter(Objects::nonNull).distinct().toList();
        if (parentIds.isEmpty()) return;
        Map<Long, Combo> parents = comboMapper.selectList(new LambdaQueryWrapper<Combo>().in(Combo::getId, parentIds))
                .stream().collect(Collectors.toMap(Combo::getId, Function.identity()));
        combos.forEach(combo -> combo.setFollowupParent(parents.get(combo.getFollowupParentId())));
    }

    @GetMapping("/admin/users")
    public PageResult<User> users(@RequestParam(defaultValue = "1") long page,
                                  @RequestParam(defaultValue = "20") long pageSize,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String role,
                                  HttpServletRequest request) {
        requireAdmin(request);
        Page<User> result = userMapper.selectPage(Page.of(PageUtil.page(page), PageUtil.adminPageSize(pageSize)), new LambdaQueryWrapper<User>()
                .eq(StringUtils.hasText(role), User::getRole, role)
                .and(StringUtils.hasText(q), w -> w.like(User::getUsername, q).or().like(User::getEmail, q))
                .orderByDesc(User::getCreatedAt));
        result.getRecords().forEach(user -> user.setPasswordHash(null));
        applyUserGovernanceStats(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    @PutMapping("/admin/users/{id}")
    @Transactional
    public User updateUser(@PathVariable Long id, @Valid @RequestBody UserAdminRequest request,
                           HttpServletRequest servletRequest) {
        User admin = requireAdmin(servletRequest);
        User user = userService.requireUser(id);
        protectCurrentAdmin(admin, user, request.role(), request.banned());
        if (StringUtils.hasText(request.role())) {
            userMapper.updateRole(user.getId(), request.role());
        }
        if (request.banned() != null) {
            userMapper.updateBanState(user.getId(), request.banned(),
                    Boolean.TRUE.equals(request.banned()) ? request.banReason() : "",
                    Boolean.TRUE.equals(request.banned()) ? request.bannedUntil() : null);
        }
        if (request.adminPermissions() != null) {
            userMapper.updateAdminPermissions(user.getId(), normalizeAdminPermissions(request.adminPermissions()));
        }
        user = userService.requireUser(id);
        adminAuditService.record(admin, "update_user", "user", user.getId(), "role=" + user.getRole() + ", perms=" + user.getAdminPermissions() + ", banned=" + user.getBanned());
        adminRealtimeService.publishAll("user:update", "users", "dashboard", "audit", "profile", "userCombos", "favoriteCombos");
        user.setPasswordHash(null);
        return user;
    }

    @PostMapping("/admin/users/{id}/ban")
    public User banUser(@PathVariable Long id, HttpServletRequest request) {
        User admin = requireAdmin(request);
        User user = userService.requireUser(id);
        protectCurrentAdmin(admin, user, user.getRole(), true);
        userMapper.updateBanState(user.getId(), true, user.getBanReason(), user.getBannedUntil());
        user = userService.requireUser(id);
        adminAuditService.record(admin, "ban_user", "user", user.getId(), "封禁用户");
        adminRealtimeService.publishAll("user:ban", "users", "dashboard", "audit", "profile");
        user.setPasswordHash(null);
        return user;
    }

    @PostMapping("/admin/users/{id}/unban")
    public User unbanUser(@PathVariable Long id, HttpServletRequest request) {
        User admin = requireAdmin(request);
        User user = userService.requireUser(id);
        userMapper.updateBanState(user.getId(), false, "", null);
        user = userService.requireUser(id);
        adminAuditService.record(admin, "unban_user", "user", user.getId(), "解封用户");
        adminRealtimeService.publishAll("user:unban", "users", "dashboard", "audit", "profile");
        user.setPasswordHash(null);
        return user;
    }

    @GetMapping("/admin/combos")
    public PageResult<Combo> combos(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "24") long pageSize,
                                    @RequestParam(required = false) Long characterId,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String controlType,
                                    @RequestParam(required = false) String q,
                                    HttpServletRequest request) {
        requireComboManager(request);
        PageResult<Combo> result = catalogService.adminCombos(page, pageSize, characterId, status, controlType, q);
        List<Combo> combos = result.list();
        applyComboRiskSignals(combos);
        applyComboParentSummaries(combos);
        List<Combo> prioritized = combos.stream()
                .sorted((left, right) -> Integer.compare(
                        right.getReviewPriority() == null ? 0 : right.getReviewPriority(),
                        left.getReviewPriority() == null ? 0 : left.getReviewPriority()))
                .toList();
        return new PageResult<>(prioritized, result.page(), result.pageSize(), result.total());
    }

    @GetMapping("/admin/characters")
    public List<CharacterInfo> adminCharacters(HttpServletRequest request) {
        requireAdmin(request);
        List<CharacterInfo> characters = catalogService.characters();
        applyCharacterContentStats(characters);
        return characters;
    }

    @PostMapping("/admin/characters")
    public CharacterInfo createCharacter(@Valid @RequestBody CharacterRequest request, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        CharacterInfo character = catalogService.createCharacter(request);
        adminRealtimeService.publishAll("character:create", "characters", "dashboard");
        return character;
    }

    @PutMapping("/admin/characters/reorder")
    public List<CharacterInfo> reorderCharacters(@Valid @RequestBody CharacterOrderRequest request,
                                                 HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        List<CharacterInfo> characters = catalogService.reorderCharacters(request.ids());
        adminRealtimeService.publishAll("character:reorder", "characters", "frames", "combos", "dashboard", "comboDetail");
        return characters;
    }

    @PutMapping("/admin/characters/{id}")
    public CharacterInfo updateCharacter(@PathVariable Long id, @Valid @RequestBody CharacterRequest request,
                                         HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        CharacterInfo character = catalogService.updateCharacter(id, request);
        adminRealtimeService.publishAll("character:update", "characters", "frames", "combos", "dashboard", "comboDetail");
        return character;
    }

    @DeleteMapping("/admin/characters/{id}")
    public void deleteCharacter(@PathVariable Long id, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        catalogService.deleteCharacter(id);
        adminRealtimeService.publishAll("character:delete", "characters", "frames", "combos", "dashboard", "comboDetail");
    }

    @GetMapping("/admin/frames")
    public PageResult<FrameData> frames(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "50") long pageSize,
                                        @RequestParam(required = false) Long characterId,
                                        @RequestParam(required = false) String controlType,
                                        @RequestParam(required = false) String q,
                                        HttpServletRequest request) {
        requireAdmin(request);
        String query = StringUtils.hasText(q) ? q.trim() : "";
        Page<FrameData> result = frameDataMapper.selectPage(Page.of(PageUtil.page(page), PageUtil.adminPageSize(pageSize)), new LambdaQueryWrapper<FrameData>()
                .eq(characterId != null, FrameData::getCharacterId, characterId)
                .eq(StringUtils.hasText(controlType), FrameData::getControlType, normalizeControlType(controlType))
                .and(StringUtils.hasText(query), wrapper -> wrapper
                        .like(FrameData::getMoveName, query)
                        .or().like(FrameData::getSourceCharacterSlug, query)
                        .or().like(FrameData::getSourceLang, query)
                        .or().like(FrameData::getProperties, query)
                        .or().like(FrameData::getMiscellaneous, query))
                .orderByAsc(FrameData::getCharacterId)
                .orderByAsc(FrameData::getId));
        applyFrameMaintenanceStats(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    @PostMapping("/admin/frames")
    public FrameData createFrame(@Valid @RequestBody FrameRequest request, HttpServletRequest servletRequest) {
        User admin = requireAdmin(servletRequest);
        FrameData frame = new FrameData();
        applyFrame(frame, request);
        frameDataMapper.insert(frame);
        recordFrameChange(frame, admin, "create", "新增帧数");
        adminAuditService.record(admin, "create_frame", "frame", frame.getId(), frame.getMoveName());
        adminRealtimeService.publishAll("frame:create", "frames", "frameOps", "dashboard", "audit", "characters");
        return frame;
    }

    @PutMapping("/admin/frames/{id}")
    public FrameData updateFrame(@PathVariable Long id, @Valid @RequestBody FrameRequest request,
                                 HttpServletRequest servletRequest) {
        User admin = requireAdmin(servletRequest);
        FrameData frame = frame(id);
        applyFrame(frame, request);
        frameDataMapper.updateById(frame);
        recordFrameChange(frame, admin, "update", "编辑帧数");
        adminAuditService.record(admin, "update_frame", "frame", frame.getId(), frame.getMoveName());
        adminRealtimeService.publishAll("frame:update", "frames", "frameOps", "dashboard", "audit", "characters");
        return frame;
    }

    @DeleteMapping("/admin/frames/{id}")
    public void deleteFrame(@PathVariable Long id, HttpServletRequest request) {
        User admin = requireAdmin(request);
        FrameData frame = frame(id);
        frameDataMapper.deleteById(id);
        recordFrameChange(frame, admin, "delete", "删除帧数");
        adminAuditService.record(admin, "delete_frame", "frame", id, frame.getMoveName());
        adminRealtimeService.publishAll("frame:delete", "frames", "frameOps", "dashboard", "audit", "characters");
    }

    @PostMapping("/admin/frames/sync-official")
    public OfficialFrameSyncService.FrameSyncSummary syncOfficialFrames(HttpServletRequest request) {
        User admin = requireAdmin(request);
        OfficialFrameSyncService.FrameSyncSummary summary = officialFrameSyncService.syncAll();
        try {
            FrameSyncLog log = frameSyncLogService.save(summary, "手动全量同步");
            adminAuditService.record(admin, "sync_official_frames", "frame_sync", log.getId(), log.getDetail());
        } catch (Exception ignored) {
            try {
                adminAuditService.record(admin, "sync_official_frames", "frame_sync", null,
                        FrameSyncLogService.detail(summary, "手动全量同步"));
            } catch (Exception ignoredAgain) {
            }
        }
        try {
            adminRealtimeService.publishAll("frame:sync", "frames", "frameOps", "dashboard", "audit", "characters");
        } catch (Exception ignored) {
        }
        return summary;
    }

    @GetMapping("/admin/frames/sync-logs")
    public List<FrameSyncLog> frameSyncLogs(HttpServletRequest request) {
        requireAdmin(request);
        return frameSyncLogMapper.selectList(new LambdaQueryWrapper<FrameSyncLog>()
                .orderByDesc(FrameSyncLog::getCreatedAt)
                .last("limit 20"));
    }

    @GetMapping("/admin/frames/change-history")
    public List<FrameChangeHistory> frameChangeHistory(HttpServletRequest request) {
        requireAdmin(request);
        return frameChangeHistoryMapper.selectList(new LambdaQueryWrapper<FrameChangeHistory>()
                .orderByDesc(FrameChangeHistory::getCreatedAt)
                .last("limit 50"));
    }

    @PostMapping("/admin/combos")
    public Combo createCombo(@Valid @RequestBody ComboRequest request, HttpServletRequest servletRequest) {
        User admin = requireComboManager(servletRequest);
        Combo combo = catalogService.createAdminCombo(request, admin);
        adminAuditService.record(admin, "create_combo", "combo", combo.getId(), combo.getStarter());
        adminRealtimeService.publishAll("combo:admin-create", "combos", "dashboard", "audit", "characters", "userCombos");
        return combo;
    }

    @PutMapping("/admin/combos/{id}")
    public Combo updateCombo(@PathVariable Long id, @Valid @RequestBody ComboRequest request, HttpServletRequest servletRequest) {
        User admin = requireComboManager(servletRequest);
        Combo combo = catalogService.updateCombo(id, request);
        adminAuditService.record(admin, "update_combo", "combo", combo.getId(), combo.getStarter());
        adminRealtimeService.publishAll("combo:admin-update", "combos", "dashboard", "audit", "characters", "userCombos", "favoriteCombos", "comboDetail");
        return combo;
    }

    @PostMapping("/admin/combos/{id}/approve")
    public Combo approveCombo(@PathVariable Long id, HttpServletRequest request) {
        User admin = requireComboManager(request);
        Combo combo = catalogService.reviewCombo(id, new ComboReviewRequest("approved", "", null, null), admin);
        notifyComboAuthor(combo, "连招审核通过", "你的连招「" + combo.getStarter() + "」已通过审核");
        adminAuditService.record(admin, "approve_combo", "combo", combo.getId(), combo.getStarter());
        adminRealtimeService.publishAll("combo:admin-approve", "combos", "dashboard", "audit", "characters", "userCombos", "notifications", "comboDetail");
        return combo;
    }

    @PutMapping("/admin/combos/{id}/review")
    public Combo reviewCombo(@PathVariable Long id, @Valid @RequestBody ComboReviewRequest review, HttpServletRequest request) {
        User admin = requireComboManager(request);
        Combo combo = catalogService.reviewCombo(id, review, admin);
        if ("rejected".equals(combo.getStatus())) {
            notifyComboAuthor(combo, "连招审核未通过", combo.getRejectionReason());
        } else if ("approved".equals(combo.getStatus())) {
            notifyComboAuthor(combo, "连招审核通过", "你的连招「" + combo.getStarter() + "」已通过审核");
        }
        adminAuditService.record(admin, "review_combo", "combo", combo.getId(), combo.getStatus() + " " + nullToEmpty(combo.getRejectionReason()));
        adminRealtimeService.publishAll("combo:admin-review", "combos", "dashboard", "audit", "characters", "userCombos", "notifications", "comboDetail");
        return combo;
    }

    @GetMapping("/admin/combos/{id}/duplicates")
    public ComboDuplicateCheckResponse duplicateCombos(@PathVariable Long id, HttpServletRequest request) {
        requireComboManager(request);
        return catalogService.duplicateCandidates(id);
    }

    @PutMapping("/admin/combos/{id}/video-review")
    public Combo updateComboVideoReview(@PathVariable Long id, @Valid @RequestBody ComboVideoReviewRequest body,
                                        HttpServletRequest request) {
        User admin = requireComboManager(request);
        String status = normalizeVideoReviewStatus(body.status());
        String videoReason = StringUtils.hasText(body.reason()) ? body.reason() : defaultVideoReviewReason(status);
        if ("video_rejected".equals(status)) {
            videoReason = "视频违规";
        }
        Combo combo = catalogService.updateComboVideoReview(id, status, videoReason, admin);
        adminAuditService.record(admin, "review_combo_video", "combo", combo.getId(), status + " " + nullToEmpty(combo.getVideoReviewReason()));
        notifyComboAuthor(combo, videoReviewTitle(status), combo.getVideoReviewReason());
        adminRealtimeService.publishAll("combo:video-review", "combos", "dashboard", "audit", "notifications", "comboDetail", "userCombos");
        return combo;
    }

    @DeleteMapping("/admin/combos/{id}")
    public void deleteCombo(@PathVariable Long id, HttpServletRequest request) {
        User admin = requireComboManager(request);
        catalogService.deleteCombo(id);
        adminAuditService.record(admin, "delete_combo", "combo", id, "删除连招");
        adminRealtimeService.publishAll("combo:admin-delete", "combos", "dashboard", "audit", "characters", "userCombos", "favoriteCombos", "comboDetail");
    }

    @GetMapping("/admin/reports")
    public PageResult<ReportAdminResponse> reports(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "30") long pageSize,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long authorId,
                                                   HttpServletRequest request) {
        requireAdmin(request);
        List<Long> authorComboIds = List.of();
        if (authorId != null) {
            authorComboIds = comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                            .select(Combo::getId)
                            .eq(Combo::getAuthorId, authorId))
                    .stream()
                    .map(Combo::getId)
                    .toList();
            if (authorComboIds.isEmpty()) {
                return new PageResult<>(List.of(), PageUtil.page(page), PageUtil.adminPageSize(pageSize), 0);
            }
        }
        Page<Report> result = reportMapper.selectPage(Page.of(PageUtil.page(page), PageUtil.adminPageSize(pageSize)), new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, "combo")
                .eq(StringUtils.hasText(status), Report::getStatus, status)
                .in(authorId != null, Report::getTargetId, authorComboIds)
                .orderByDesc(Report::getCreatedAt));
        Map<Long, Combo> comboTargets = loadReportComboTargets(result.getRecords());
        Map<Long, CharacterInfo> targetCharacters = loadTargetCharacters(comboTargets.values());
        Map<Long, User> userTargets = loadReportUserTargets(result.getRecords());
        List<ReportAdminResponse> records = result.getRecords().stream()
                .map(report -> toReportAdminResponse(report, comboTargets, targetCharacters, userTargets))
                .toList();
        return new PageResult<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @PutMapping("/admin/reports/{id}")
    public Report handleReport(@PathVariable Long id, @Valid @RequestBody ReportActionRequest action,
                               HttpServletRequest servletRequest) {
        User admin = requireAdmin(servletRequest);
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException("举报不存在");
        }
        String nextStatus = normalizeReportStatus(action.status());
        report.setStatus(nextStatus);
        report.setResolution(StringUtils.hasText(action.resolution()) ? action.resolution() : defaultReportResolution(nextStatus));
        report.setHandlerId(admin.getId());
        report.setHandler(admin.getUsername());
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);
        adminAuditService.record(admin, "handle_report", "report", report.getId(), report.getStatus() + " " + nullToEmpty(report.getResolution()));
        adminRealtimeService.publish("report:update", "reports", "dashboard", "audit");
        return report;
    }

    @PutMapping("/admin/reports/batch")
    @Transactional
    public Map<String, Object> handleReportsBatch(@Valid @RequestBody ReportBatchActionRequest action,
                                                  HttpServletRequest servletRequest) {
        User admin = requireAdmin(servletRequest);
        if (action.ids() == null || action.ids().isEmpty()) {
            throw new BizException("请选择要处理的举报");
        }
        String nextStatus = normalizeReportStatus(action.status());
        String resolution = StringUtils.hasText(action.resolution()) ? action.resolution() : defaultReportResolution(nextStatus);
        LocalDateTime handledAt = LocalDateTime.now();
        List<Long> ids = action.ids().stream().filter(Objects::nonNull).distinct().toList();
        int updated = ids.isEmpty() ? 0 : reportMapper.update(null, new LambdaUpdateWrapper<Report>()
                .in(Report::getId, ids)
                .set(Report::getStatus, nextStatus)
                .set(Report::getResolution, resolution)
                .set(Report::getHandlerId, admin.getId())
                .set(Report::getHandler, admin.getUsername())
                .set(Report::getHandledAt, handledAt));
        adminAuditService.record(admin, "batch_handle_reports", "report", null, nextStatus + " " + updated + " 条");
        adminRealtimeService.publish("report:batch-update", "reports", "dashboard", "audit");
        return Map.of("updated", updated);
    }

    @GetMapping("/admin/feedbacks")
    public PageResult<FeedbackAdminResponse> feedbacks(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "30") long pageSize,
                                                       @RequestParam(required = false) String status,
                                                       HttpServletRequest request) {
        requireAdmin(request);
        Page<Feedback> result = feedbackMapper.selectPage(Page.of(PageUtil.page(page), PageUtil.adminPageSize(pageSize)), new LambdaQueryWrapper<Feedback>()
                .eq(StringUtils.hasText(status), Feedback::getStatus, status)
                .orderByDesc(Feedback::getCreatedAt));
        List<FeedbackAdminResponse> records = result.getRecords().stream()
                .map(this::toFeedbackAdminResponse)
                .toList();
        return new PageResult<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @PutMapping("/admin/feedbacks/{id}")
    public Feedback handleFeedback(@PathVariable Long id, @Valid @RequestBody FeedbackActionRequest action,
                                   HttpServletRequest servletRequest) {
        User admin = requireAdmin(servletRequest);
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BizException("反馈不存在");
        }
        String nextStatus = normalizeFeedbackStatus(action.status());
        feedback.setStatus(nextStatus);
        feedback.setResolution(StringUtils.hasText(action.resolution()) ? action.resolution() : defaultFeedbackResolution(nextStatus));
        feedback.setHandlerId(admin.getId());
        feedback.setHandler(admin.getUsername());
        feedback.setHandledAt(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
        adminAuditService.record(admin, "handle_feedback", "feedback", feedback.getId(), feedback.getStatus() + " " + nullToEmpty(feedback.getResolution()));
        if (StringUtils.hasText(feedback.getResolution()) && feedback.getUserId() != null) {
            notificationService.notifyUserById(feedback.getUserId(), admin.getUsername(), "feedback",
                    "反馈处理进度更新", feedback.getResolution(), "/feedback");
        } else if (StringUtils.hasText(feedback.getUsername()) && StringUtils.hasText(feedback.getResolution())) {
            notificationService.notifyUser(feedback.getUsername(), admin.getUsername(), "feedback",
                    "反馈处理进度更新", feedback.getResolution(), "/feedback");
        }
        adminRealtimeService.publish("feedback:update", "feedbacks", "dashboard", "audit");
        return feedback;
    }

    @PutMapping("/admin/feedbacks/batch")
    @Transactional
    public Map<String, Object> handleFeedbacksBatch(@Valid @RequestBody FeedbackBatchActionRequest action,
                                                    HttpServletRequest servletRequest) {
        User admin = requireAdmin(servletRequest);
        if (action.ids() == null || action.ids().isEmpty()) {
            throw new BizException("请选择要处理的反馈");
        }
        String nextStatus = normalizeFeedbackStatus(action.status());
        String resolution = StringUtils.hasText(action.resolution()) ? action.resolution() : defaultFeedbackResolution(nextStatus);
        LocalDateTime handledAt = LocalDateTime.now();
        List<Long> ids = action.ids().stream().filter(Objects::nonNull).distinct().toList();
        int updated = ids.isEmpty() ? 0 : feedbackMapper.update(null, new LambdaUpdateWrapper<Feedback>()
                .in(Feedback::getId, ids)
                .set(Feedback::getStatus, nextStatus)
                .set(Feedback::getResolution, resolution)
                .set(Feedback::getHandlerId, admin.getId())
                .set(Feedback::getHandler, admin.getUsername())
                .set(Feedback::getHandledAt, handledAt));
        adminAuditService.record(admin, "batch_handle_feedbacks", "feedback", null, nextStatus + " " + updated + " 条");
        adminRealtimeService.publish("feedback:batch-update", "feedbacks", "dashboard", "audit");
        return Map.of("updated", updated);
    }

    @GetMapping("/admin/announcements")
    public List<Announcement> announcements(HttpServletRequest request) {
        requireAdmin(request);
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreatedAt));
    }

    @PostMapping("/admin/announcements")
    public Announcement createAnnouncement(@Valid @RequestBody AnnouncementRequest body, HttpServletRequest request) {
        User admin = requireAdmin(request);
        Announcement announcement = new Announcement();
        announcement.setTitle(body.title());
        announcement.setContent(body.content());
        announcement.setLevel(StringUtils.hasText(body.level()) ? body.level() : "info");
        announcement.setPublished(!Boolean.FALSE.equals(body.published()));
        announcement.setCreatedBy(admin.getUsername());
        announcement.setCreatedAt(LocalDateTime.now());
        announcementMapper.insert(announcement);
        adminAuditService.record(admin, "create_announcement", "announcement", announcement.getId(), announcement.getTitle());
        adminRealtimeService.publishAll("announcement:create", "announcements", "dashboard", "audit");
        return announcement;
    }

    @PutMapping("/admin/announcements/{id}")
    public Announcement updateAnnouncement(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest body,
                                           HttpServletRequest request) {
        User admin = requireAdmin(request);
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BizException("指南不存在");
        }
        announcement.setTitle(body.title());
        announcement.setContent(body.content());
        announcement.setLevel(StringUtils.hasText(body.level()) ? body.level() : "info");
        announcement.setPublished(!Boolean.FALSE.equals(body.published()));
        announcementMapper.updateById(announcement);
        adminAuditService.record(admin, "update_announcement", "announcement", announcement.getId(), announcement.getTitle());
        adminRealtimeService.publishAll("announcement:update", "announcements", "dashboard", "audit");
        return announcement;
    }

    @DeleteMapping("/admin/announcements/{id}")
    public void deleteAnnouncement(@PathVariable Long id, HttpServletRequest request) {
        User admin = requireAdmin(request);
        announcementMapper.deleteById(id);
        adminAuditService.record(admin, "delete_announcement", "announcement", id, "");
        adminRealtimeService.publishAll("announcement:delete", "announcements", "dashboard", "audit");
    }

    @PostMapping("/admin/notifications/broadcast")
    public Map<String, Object> broadcastNotification(@Valid @RequestBody NotificationBroadcastRequest body,
                                                    HttpServletRequest request) {
        User admin = requireAdmin(request);
        int count = notificationService.broadcast(body.title(), body.content(), body.targetUrl(), body.username());
        adminAuditService.record(admin, "broadcast_notification", "notification", null, "发送给 " + count + " 个用户");
        adminRealtimeService.publishAll("notification:broadcast", "audit", "notifications");
        return Map.of("sent", count);
    }

    @GetMapping("/admin/audit-logs")
    public PageResult<AdminAuditLog> auditLogs(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "30") long pageSize,
                                               @RequestParam(required = false) String targetType,
                                               @RequestParam(required = false) String action,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                               HttpServletRequest request) {
        requireAdmin(request);
        Page<AdminAuditLog> result = adminAuditLogMapper.selectPage(Page.of(PageUtil.page(page), PageUtil.adminPageSize(pageSize)),
                new LambdaQueryWrapper<AdminAuditLog>()
                        .eq(StringUtils.hasText(targetType), AdminAuditLog::getTargetType, targetType)
                        .eq(StringUtils.hasText(action), AdminAuditLog::getAction, action)
                        .ge(startDate != null, AdminAuditLog::getCreatedAt, startDate == null ? null : startDate.atStartOfDay())
                        .lt(endDate != null, AdminAuditLog::getCreatedAt, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                        .orderByDesc(AdminAuditLog::getCreatedAt));
        return new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal());
    }

    private FrameData frame(Long id) {
        FrameData frame = frameDataMapper.selectById(id);
        if (frame == null) {
            throw new BizException("帧数数据不存在");
        }
        return frame;
    }

    private static String frameSyncDetail(OfficialFrameSyncService.FrameSyncSummary summary) {
        String base = "成功 " + summary.successCount() + "/" + summary.totalCharacters()
                + "，导入 " + summary.importedCount() + " 条";
        List<OfficialFrameSyncService.FrameSyncResult> failures = summary.results().stream()
                .filter(result -> !result.success())
                .toList();
        if (failures.isEmpty()) {
            return base;
        }
        String failedDetail = failures.stream()
                .limit(6)
                .map(result -> result.name() + ": " + nullToEmpty(result.error()))
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
        if (failures.size() > 6) {
            failedDetail += " | 另有 " + (failures.size() - 6) + " 个角色失败";
        }
        return base + "；失败：" + failedDetail;
    }

    private static void applyFrame(FrameData frame, FrameRequest request) {
        frame.setCharacterId(request.characterId());
        frame.setControlType(normalizeControlType(request.controlType()));
        frame.setMoveName(request.moveName());
        frame.setStartup(request.startup());
        frame.setActive(request.active());
        frame.setRecovery(request.recovery());
        frame.setOnBlock(request.onBlock());
        frame.setOnHit(request.onHit());
        frame.setCancel(request.cancel());
        frame.setDamage(request.damage());
        frame.setComboScaling(request.comboScaling());
        frame.setDriveGainOnHit(request.driveGainOnHit());
        frame.setDriveLossOnBlock(request.driveLossOnBlock());
        frame.setDriveLossOnPunishCounter(request.driveLossOnPunishCounter());
        frame.setSuperArtGain(request.superArtGain());
        frame.setProperties(request.properties());
        frame.setMiscellaneous(request.miscellaneous());
        frame.setSourceUrl(request.sourceUrl());
        frame.setSourceCharacterSlug(request.sourceCharacterSlug());
        frame.setSourceLang(request.sourceLang());
        frame.setDisplayOrder(request.displayOrder());
        frame.setManualOverride(true);
    }

    private static String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        return value.trim();
    }

    private void recordFrameChange(FrameData frame, User admin, String action, String detail) {
        FrameChangeHistory history = new FrameChangeHistory();
        history.setFrameId(frame.getId());
        history.setCharacterId(frame.getCharacterId());
        history.setMoveName(frame.getMoveName());
        history.setAction(action);
        history.setAdminName(admin == null ? "" : admin.getUsername());
        history.setDetail(detail);
        history.setCreatedAt(LocalDateTime.now());
        frameChangeHistoryMapper.insert(history);
    }

    private void notifyComboAuthor(Combo combo, String title, String content) {
        if (combo == null) {
            return;
        }
        if (combo.getAuthorId() != null) {
            notificationService.notifyUserById(combo.getAuthorId(), "admin", "combo_review", title, content, "/combos/" + combo.getId());
            return;
        }
        if (!StringUtils.hasText(combo.getAuthor())) {
            return;
        }
        notificationService.notifyUser(combo.getAuthor(), "admin", "combo_review", title, content, "/combos/" + combo.getId());
    }

    private ReportAdminResponse toReportAdminResponse(Report report, Map<Long, Combo> comboTargets,
                                                      Map<Long, CharacterInfo> targetCharacters,
                                                      Map<Long, User> userTargets) {
        TargetSummary target = reportTargetSummary(report, comboTargets, targetCharacters, userTargets);
        return new ReportAdminResponse(
                report.getId(),
                report.getReporterId(),
                report.getReporter(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getHandlerId(),
                report.getHandler(),
                report.getResolution(),
                report.getCreatedAt(),
                report.getHandledAt(),
                target.title(),
                target.subtitle(),
                target.status(),
                target.owner(),
                target.url()
        );
    }

    private FeedbackAdminResponse toFeedbackAdminResponse(Feedback feedback) {
        return new FeedbackAdminResponse(
                feedback.getId(),
                feedback.getUserId(),
                feedback.getUsername(),
                feedback.getReason(),
                feedback.getDetail(),
                feedback.getStatus(),
                feedback.getHandlerId(),
                feedback.getHandler(),
                feedback.getResolution(),
                feedback.getCreatedAt(),
                feedback.getHandledAt()
        );
    }

    private TargetSummary reportTargetSummary(Report report, Map<Long, Combo> comboTargets,
                                              Map<Long, CharacterInfo> targetCharacters,
                                              Map<Long, User> userTargets) {
        if (report == null || report.getTargetId() == null) {
            return TargetSummary.missing();
        }
        String type = report.getTargetType();
        Long id = report.getTargetId();
        if ("combo".equals(type)) {
            Combo combo = comboTargets.get(id);
            if (combo == null) {
                return TargetSummary.deleted("连招已不存在");
            }
            String characterName = "";
            if (combo.getCharacterId() != null) {
                CharacterInfo character = targetCharacters.get(combo.getCharacterId());
                characterName = character == null ? "" : character.getName();
            }
            String starter = StringUtils.hasText(combo.getStarter()) ? combo.getStarter() : "未命名连招";
            String title = StringUtils.hasText(characterName) ? "连招 · " + characterName + " · " + starter : "连招 · " + starter;
            String subtitle = StringUtils.hasText(combo.getRoute()) ? combo.getRoute() : combo.getComboText();
            String owner = StringUtils.hasText(combo.getAuthor()) ? combo.getAuthor() : "未记录作者";
            return new TargetSummary(title, subtitle, combo.getStatus(), owner, "/admin?section=combos&targetId=" + combo.getId());
        }
        if ("user".equals(type)) {
            User user = userTargets.get(id);
            if (user == null) {
                return TargetSummary.deleted("用户已不存在");
            }
            String title = "用户 · " + user.getUsername();
            String subtitle = StringUtils.hasText(user.getEmail()) ? user.getEmail() : nullToEmpty(user.getBio());
            return new TargetSummary(title, subtitle, Boolean.TRUE.equals(user.getBanned()) ? "banned" : "active", user.getUsername(), "/admin?section=users&targetId=" + user.getId());
        }
        return new TargetSummary("对象 #" + id, "", "", "", "");
    }

    private Map<Long, Combo> loadReportComboTargets(List<Report> reports) {
        List<Long> ids = reports.stream().filter(report -> "combo".equals(report.getTargetType()))
                .map(Report::getTargetId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return comboMapper.selectList(new LambdaQueryWrapper<Combo>().in(Combo::getId, ids))
                .stream().collect(Collectors.toMap(Combo::getId, Function.identity()));
    }

    private Map<Long, CharacterInfo> loadTargetCharacters(java.util.Collection<Combo> combos) {
        List<Long> ids = combos.stream().map(Combo::getCharacterId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return characterMapper.selectList(new LambdaQueryWrapper<CharacterInfo>().in(CharacterInfo::getId, ids))
                .stream().collect(Collectors.toMap(CharacterInfo::getId, Function.identity()));
    }

    private Map<Long, User> loadReportUserTargets(List<Report> reports) {
        List<Long> ids = reports.stream().filter(report -> "user".equals(report.getTargetType()))
                .map(Report::getTargetId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, ids))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private static String normalizeReportStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "resolved";
        }
        String normalized = status.trim();
        if (!List.of("pending", "processing", "resolved", "rejected").contains(normalized)) {
            throw new BizException("举报状态不合法");
        }
        return normalized;
    }

    private static String defaultReportResolution(String status) {
        return "rejected".equals(status) ? "管理员已驳回" : "管理员已处理";
    }

    private static String normalizeFeedbackStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "resolved";
        }
        String normalized = status.trim();
        if (!List.of("pending", "processing", "resolved", "rejected").contains(normalized)) {
            throw new BizException("反馈状态不合法");
        }
        return normalized;
    }

    private static String defaultFeedbackResolution(String status) {
        return "rejected".equals(status) ? "管理员已驳回" : "管理员已处理";
    }

    private static String normalizeVideoReviewStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "video_approved";
        }
        String normalized = status.trim();
        if (!List.of("unchecked", "video_checking", "video_approved", "video_rejected").contains(normalized)) {
            throw new BizException("视频审核状态不合法");
        }
        return normalized;
    }

    private static String defaultVideoReviewReason(String status) {
        return switch (status) {
            case "video_rejected" -> "视频内容或格式未通过审核";
            case "video_checking" -> "视频正在复核中";
            case "video_approved" -> "视频审核通过";
            default -> "";
        };
    }

    private static String videoReviewTitle(String status) {
        return switch (status) {
            case "video_rejected" -> "连招视频未通过审核";
            case "video_approved" -> "连招视频已通过审核";
            default -> "连招视频状态已更新";
        };
    }

    private record TargetSummary(String title, String subtitle, String status, String owner, String url) {
        static TargetSummary missing() {
            return new TargetSummary("对象信息缺失", "", "", "", "");
        }

        static TargetSummary deleted(String title) {
            return new TargetSummary(title, "", "deleted", "", "");
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean adminComboMatchesStatus(Combo combo, String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return switch (status) {
            case "reviewed" -> "approved".equals(combo.getStatus());
            case "rejected" -> "rejected".equals(combo.getStatus());
            case "manual_review" -> "manual_review".equals(combo.getStatus());
            case "pending" -> "pending".equals(combo.getStatus());
            default -> status.equals(combo.getStatus());
        };
    }

    private static String normalizeControlType(String value) {
        if (!StringUtils.hasText(value)) {
            return "classic";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("world") || normalized.contains("环球")) {
            return "world-tour";
        }
        return normalized.contains("modern") || normalized.contains("现代") || "m".equals(normalized) || "mod".equals(normalized)
                ? "modern"
                : "classic";
    }

    private static void protectCurrentAdmin(User admin, User user, String requestedRole, Boolean requestedBanned) {
        if (admin == null || user == null || !java.util.Objects.equals(admin.getId(), user.getId())) {
            return;
        }
        if (StringUtils.hasText(requestedRole) && !"admin".equals(requestedRole)) {
            throw new BizException("不能降级当前登录管理员");
        }
        if (Boolean.TRUE.equals(requestedBanned)) {
            throw new BizException("不能封禁当前登录管理员");
        }
    }

    private User requireAdmin(HttpServletRequest request) {
        User user = currentUserService.requireAdminSession(request);
        if (!"admin".equals(user.getRole())) {
            throw new BizException("需要管理员权限");
        }
        return user;
    }

    private User requireComboManager(HttpServletRequest request) {
        User user = currentUserService.requireAdminSession(request);
        if (!isAdmin(user) && !hasAdminPermission(user, COMBO_REVIEW_PERMISSION)) {
            throw new BizException("需要连招审核管理权限");
        }
        return user;
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

    private static String normalizeAdminPermissions(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        boolean comboReview = List.of(value.split(",")).stream()
                .map(String::trim)
                .anyMatch(COMBO_REVIEW_PERMISSION::equals);
        return comboReview ? COMBO_REVIEW_PERMISSION : "";
    }
}
