package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.hubdemo.common.PageResult;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.AdminDtos.ComboReviewRequest;
import com.example.hubdemo.dto.ComboDtos.ComboFavoriteResponse;
import com.example.hubdemo.dto.ComboDtos.ComboFilterOptions;
import com.example.hubdemo.dto.ComboDtos.ComboLikeResponse;
import com.example.hubdemo.dto.ComboDtos.ComboRequest;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCheckRequest;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCheckResponse;
import com.example.hubdemo.dto.ComboDtos.WeeklyContributor;
import com.example.hubdemo.dto.AdminDtos.CharacterRequest;
import com.example.hubdemo.dto.AdminDtos.CharacterDataRequest;
import com.example.hubdemo.entity.CharacterInfo;
import com.example.hubdemo.entity.CharacterData;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.ComboFavorite;
import com.example.hubdemo.entity.ComboFollowup;
import com.example.hubdemo.entity.ComboLike;
import com.example.hubdemo.entity.FrameData;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.CharacterDataMapper;
import com.example.hubdemo.mapper.ComboFavoriteMapper;
import com.example.hubdemo.mapper.ComboFollowupMapper;
import com.example.hubdemo.mapper.ComboLikeMapper;
import com.example.hubdemo.mapper.ComboMapper;
import com.example.hubdemo.mapper.FrameDataMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.util.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.text.Normalizer;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogService {
    private static final Set<String> PRESSURE_TYPES = Set.of(
            "meaty-strike", "throw-pressure", "shimmy", "safe-jump", "side-switch-pressure"
    );
    private static final Set<String> NORMAL_COMBO_TAGS = Set.of(
            "counter-hit", "punish-counter", "drive-impact", "corner", "central", "anti-air", "air-hit",
            "throw-starter", "low-starter", "drive-rush", "di-wall-splat", "side-switch", "corner-carry",
            "oki", "knockdown", "hit-confirm", "fun"
    );

    private final CharacterMapper characterMapper;
    private final CharacterDataMapper characterDataMapper;
    private final FrameDataMapper frameDataMapper;
    private final ComboMapper comboMapper;
    private final ComboLikeMapper comboLikeMapper;
    private final ComboFavoriteMapper comboFavoriteMapper;
    private final ComboFollowupMapper comboFollowupMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final AdminRealtimeService adminRealtimeService;
    private final UploadOwnershipService uploadOwnershipService;
    private final ComboDuplicateService comboDuplicateService;

    public CatalogService(CharacterMapper characterMapper, CharacterDataMapper characterDataMapper,
                           FrameDataMapper frameDataMapper, ComboMapper comboMapper,
                           ComboLikeMapper comboLikeMapper, ComboFavoriteMapper comboFavoriteMapper,
                           ComboFollowupMapper comboFollowupMapper, UserMapper userMapper, NotificationService notificationService,
                           AdminRealtimeService adminRealtimeService, UploadOwnershipService uploadOwnershipService,
                           ComboDuplicateService comboDuplicateService) {
        this.characterMapper = characterMapper;
        this.characterDataMapper = characterDataMapper;
        this.frameDataMapper = frameDataMapper;
        this.comboMapper = comboMapper;
        this.comboLikeMapper = comboLikeMapper;
        this.comboFavoriteMapper = comboFavoriteMapper;
        this.comboFollowupMapper = comboFollowupMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.adminRealtimeService = adminRealtimeService;
        this.uploadOwnershipService = uploadOwnershipService;
        this.comboDuplicateService = comboDuplicateService;
    }

    public List<CharacterInfo> characters() {
        List<CharacterInfo> characters = characterMapper.selectList(characterOrderQuery());
        attachCharacterData(characters);
        return characters;
    }

    public List<WeeklyContributor> weeklyContributors() {
        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDateTime weekStart = weekStartDate.atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        return comboMapper.selectWeeklyContributors(weekStart, weekEnd).stream()
                .map(row -> new WeeklyContributor(
                        ((Number) row.get("userId")).longValue(),
                        String.valueOf(row.getOrDefault("username", "")),
                        String.valueOf(row.getOrDefault("avatar", "")),
                        ((Number) row.get("comboCount")).longValue()
                ))
                .toList();
    }

    @Transactional
    public CharacterInfo createCharacter(CharacterRequest request) {
        CharacterInfo character = new CharacterInfo();
        character.setName(request.name());
        character.setAvatar(request.avatar());
        character.setDescription(request.description());
        character.setArchetype(request.archetype());
        character.setDisplayOrder(nextCharacterDisplayOrder());
        characterMapper.insert(character);
        character.setCharacterData(saveCharacterData(character.getId(), request.characterData(), true));
        return character;
    }

    @Transactional
    public CharacterInfo updateCharacter(Long id, CharacterRequest request) {
        CharacterInfo character = character(id);
        character.setName(request.name());
        character.setAvatar(request.avatar());
        character.setDescription(request.description());
        character.setArchetype(request.archetype());
        characterMapper.updateById(character);
        if (request.characterData() != null || character.getCharacterData() == null) {
            character.setCharacterData(saveCharacterData(id, request.characterData(), true));
        }
        return character;
    }

    @Transactional
    public void deleteCharacter(Long id) {
        characterMapper.deleteById(id);
    }

    @Transactional
    public List<CharacterInfo> reorderCharacters(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要排序的角色");
        }
        LinkedHashSet<Long> orderedIds = new LinkedHashSet<>();
        ids.stream()
                .filter(id -> id != null && id > 0)
                .forEach(orderedIds::add);
        List<CharacterInfo> characters = characters();
        if (orderedIds.size() != characters.size()) {
            throw new BizException("排序列表与角色数量不一致，请刷新后重试");
        }
        Set<Long> existingIds = characters.stream()
                .map(CharacterInfo::getId)
                .collect(Collectors.toSet());
        if (!existingIds.equals(orderedIds)) {
            throw new BizException("排序列表包含不存在的角色，请刷新后重试");
        }
        int order = 1;
        for (Long id : orderedIds) {
            CharacterInfo character = new CharacterInfo();
            character.setId(id);
            character.setDisplayOrder(order++);
            characterMapper.updateById(character);
        }
        return characters();
    }

    public CharacterInfo character(Long id) {
        CharacterInfo character = characterMapper.selectById(id);
        if (character == null) {
            throw new BizException("角色不存在");
        }
        character.setCharacterData(characterDataMapper.selectById(id));
        return character;
    }

    private void attachCharacterData(List<CharacterInfo> characters) {
        if (characters == null || characters.isEmpty()) {
            return;
        }
        List<Long> characterIds = characters.stream().map(CharacterInfo::getId).toList();
        List<CharacterData> rows = characterDataMapper.selectList(new LambdaQueryWrapper<CharacterData>()
                .in(CharacterData::getCharacterId, characterIds));
        Map<Long, CharacterData> byCharacterId = (rows == null ? List.<CharacterData>of() : rows).stream()
                .collect(Collectors.toMap(CharacterData::getCharacterId, Function.identity()));
        characters.forEach(character -> character.setCharacterData(byCharacterId.get(character.getId())));
    }

    private CharacterData saveCharacterData(Long characterId, CharacterDataRequest request,
                                            boolean createTemplateWhenMissing) {
        if (characterId == null) {
            throw new BizException("角色基础数据保存失败");
        }
        CharacterData data = characterDataMapper.selectById(characterId);
        boolean isNew = data == null;
        if (isNew) {
            if (!createTemplateWhenMissing && request == null) {
                return null;
            }
            data = new CharacterData();
            data.setCharacterId(characterId);
        }
        if (request != null) {
            data.setHp(request.hp());
            data.setThrowRange(request.throwRange());
            data.setForwardWalkSpeed(request.forwardWalkSpeed());
            data.setBackWalkSpeed(request.backWalkSpeed());
            data.setForwardDashSpeed(request.forwardDashSpeed());
            data.setBackDashSpeed(request.backDashSpeed());
            data.setForwardDashDistance(request.forwardDashDistance());
            data.setBackDashDistance(request.backDashDistance());
            data.setJumpSpeed(request.jumpSpeed());
            data.setForwardJumpDistance(request.forwardJumpDistance());
            data.setBackJumpDistance(request.backJumpDistance());
            data.setFastestNormal(request.fastestNormal());
            data.setSourceName(request.sourceName());
            data.setSourceUrl(request.sourceUrl());
        }
        data.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            characterDataMapper.insert(data);
        } else {
            characterDataMapper.updateById(data);
        }
        return data;
    }

    public List<FrameData> frames(Long characterId) {
        return frameDataMapper.selectList(new LambdaQueryWrapper<FrameData>()
                .eq(FrameData::getCharacterId, characterId)
                .orderByAsc(FrameData::getControlType)
                .orderByAsc(FrameData::getDisplayOrder)
                .orderByAsc(FrameData::getId));
    }

    public int nextCharacterDisplayOrder() {
        CharacterInfo last = characterMapper.selectOne(new LambdaQueryWrapper<CharacterInfo>()
                .orderByDesc(CharacterInfo::getDisplayOrder)
                .orderByDesc(CharacterInfo::getId)
                .last("limit 1"));
        int maxOrder = last == null || last.getDisplayOrder() == null ? 0 : last.getDisplayOrder();
        return maxOrder + 1;
    }

    private static LambdaQueryWrapper<CharacterInfo> characterOrderQuery() {
        return new LambdaQueryWrapper<CharacterInfo>()
                .orderByAsc(CharacterInfo::getDisplayOrder)
                .orderByAsc(CharacterInfo::getId);
    }

    public List<Combo> combos(String q, Long characterId, String type) {
        return combos(q, characterId, type, "approved");
    }

    public List<Combo> combos(String q, Long characterId, String type, String sort, User viewer) {
        return combos(q, characterId, type, "approved", sort, viewer);
    }

    public List<Combo> adminCombos() {
        return combos(null, null, null, null, null, null);
    }

    public PageResult<Combo> adminCombos(long page, long pageSize, Long characterId, String status,
                                         String controlType, String q) {
        String effectiveStatus = "reviewed".equals(status) ? "approved" : status;
        LambdaQueryWrapper<Combo> query = new LambdaQueryWrapper<Combo>()
                .eq(characterId != null, Combo::getCharacterId, characterId)
                .eq(StringUtils.hasText(effectiveStatus), Combo::getStatus, effectiveStatus)
                .eq(StringUtils.hasText(controlType), Combo::getControlType, normalizeControlType(controlType))
                .and(StringUtils.hasText(q), wrapper -> wrapper.like(Combo::getStarter, q)
                        .or().like(Combo::getRoute, q)
                        .or().like(Combo::getComboText, q))
                .orderByDesc(Combo::getSubmittedAt)
                .orderByDesc(Combo::getId);
        Page<Combo> result = comboMapper.selectPage(Page.of(PageUtil.page(page), PageUtil.adminPageSize(pageSize)), query);
        return new PageResult<>(withViewerState(result.getRecords(), null), result.getCurrent(), result.getSize(), result.getTotal());
    }

    public List<Combo> combos(String q, Long characterId, String type, String status) {
        return combos(q, characterId, type, status, null, null);
    }

    public List<Combo> combos(String q, Long characterId, String type, String status, String sort, User viewer) {
        LambdaQueryWrapper<Combo> query = new LambdaQueryWrapper<Combo>()
                .eq(characterId != null, Combo::getCharacterId, characterId)
                .eq(StringUtils.hasText(status), Combo::getStatus, status)
                .and(StringUtils.hasText(q), w -> w.like(Combo::getStarter, q)
                        .or().like(Combo::getRoute, q)
                        .or().like(Combo::getComboText, q));
        if ("likes".equals(sort)) {
            query.orderByDesc(Combo::getLikes).orderByDesc(Combo::getId);
        } else {
            query.orderByDesc(Combo::getDamage).orderByDesc(Combo::getId);
        }
        List<Combo> combos = comboMapper.selectList(query);
        if (StringUtils.hasText(type)) {
            combos = combos.stream()
                    .filter(combo -> comboHasTag(combo, type))
                    .toList();
        }
        if ("approved".equals(status)) {
            combos = withoutFollowupCombos(combos);
        }
        return withViewerState(combos, viewer);
    }

    public Combo combo(Long id) {
        Combo combo = comboMapper.selectById(id);
        if (combo == null) {
            throw new BizException("连招不存在");
        }
        return combo;
    }

    public Combo combo(Long id, User viewer) {
        return withViewerState(List.of(combo(id)), viewer).get(0);
    }

    public Combo visibleCombo(Long id, User viewer) {
        Combo combo = combo(id, viewer);
        if (!"approved".equals(combo.getStatus())
                && (viewer == null || !viewer.getId().equals(combo.getAuthorId()))) {
            throw new BizException("连招不存在或无权查看");
        }
        return combo;
    }

    public List<Combo> followupCombos(Long parentComboId, User viewer) {
        Combo parent = visibleCombo(parentComboId, viewer);
        List<Long> followupIds = comboFollowupMapper.selectList(new LambdaQueryWrapper<ComboFollowup>()
                        .eq(ComboFollowup::getParentComboId, parent.getId())
                        .orderByDesc(ComboFollowup::getCreatedAt)
                        .orderByDesc(ComboFollowup::getId))
                .stream()
                .map(ComboFollowup::getFollowupComboId)
                .toList();
        if (followupIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Combo> combosById = comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                        .in(Combo::getId, followupIds)
                        .eq(Combo::getStatus, "approved")
                        .eq(Combo::getCharacterId, parent.getCharacterId()))
                .stream()
                .collect(Collectors.toMap(Combo::getId, Function.identity()));
        List<Combo> ordered = followupIds.stream()
                .map(combosById::get)
                .filter(combo -> combo != null)
                .toList();
        return withViewerState(ordered, viewer);
    }

    @Transactional
    public Combo createCombo(ComboRequest request) {
        return createCombo(request, "manual_review", null);
    }

    @Transactional
    public Combo createCombo(ComboRequest request, User currentUser) {
        return createCombo(request, "manual_review", currentUser);
    }

    @Transactional
    public Combo createAdminCombo(ComboRequest request) {
        return createCombo(request, "approved", null);
    }

    @Transactional
    public Combo createAdminCombo(ComboRequest request, User currentUser) {
        return createCombo(request, "approved", currentUser);
    }

    private Combo createCombo(ComboRequest request, String status, User currentUser) {
        requireVideo(request);
        if (currentUser != null && "manual_review".equals(status)) {
            uploadOwnershipService.requireOwnedComboVideo(request.videoUrl(), currentUser.getId());
        }
        validateComboCharacterSelection(request);
        comboDuplicateService.requireNoExactDuplicate(request, null);
        Combo combo = new Combo();
        apply(combo, request);
        combo.setDedupeKey(comboDuplicateService.dedupeKey(request));
        if (currentUser != null) {
            combo.setAuthorId(currentUser.getId());
            combo.setAuthor(currentUser.getUsername());
        }
        combo.setLikes(0);
        combo.setFavorites(0);
        combo.setStatus(status);
        combo.setSubmittedAt(LocalDateTime.now());
        combo.setManualReviewReason("manual_review".equals(status) && currentUser != null ? "用户投稿，等待管理员审核" : "");
        combo.setVideoReviewStatus("unchecked");
        combo.setVideoReviewReason("");
        combo.setVideoReviewedAt(null);
        combo.setCreatedAt(LocalDate.now());
        comboMapper.insert(combo);
        syncFollowupParent(combo, request, currentUser);
        if ("manual_review".equals(status) && currentUser != null) {
            notificationService.notifyUserById(currentUser.getId(), "system", "combo_review",
                    "连招已提交审核", "你的连招已提交成功，正在等待管理员审核。", "/my-combos");
        }
        adminRealtimeService.publishAll("combo:create", "combos", "dashboard", "characters", "userCombos");
        return combo;
    }

    @Transactional
    public Combo updateCombo(Long id, ComboRequest request) {
        Combo combo = combo(id);
        requireVideo(request);
        validateComboCharacterSelection(request);
        comboDuplicateService.requireNoExactDuplicate(request, id);
        apply(combo, request);
        combo.setDedupeKey(comboDuplicateService.dedupeKey(request));
        comboMapper.updateContentFields(combo);
        syncFollowupParent(combo, request, null);
        adminRealtimeService.publishAll("combo:update", "combos", "dashboard", "characters", "userCombos", "comboDetail");
        return combo;
    }

    @Transactional
    public Combo updateCombo(Long id, ComboRequest request, User currentUser) {
        Combo combo = combo(id);
        requireOwnerOrAdmin(combo, currentUser, "只能编辑自己发布的连招");
        requireRejectedOwnerCombo(combo, currentUser, "只有被驳回的连招可以修改后重新提交");
        requireVideo(request);
        uploadOwnershipService.requireOwnedComboVideo(request.videoUrl(), currentUser.getId());
        validateComboCharacterSelection(request);
        comboDuplicateService.requireNoExactDuplicate(request, id);
        apply(combo, request);
        combo.setDedupeKey(comboDuplicateService.dedupeKey(request));
        comboMapper.updateContentFields(combo);
        LocalDateTime submittedAt = LocalDateTime.now();
        combo.setStatus("manual_review");
        combo.setRejectionReason("");
        combo.setReviewedBy("");
        combo.setReviewedAt(null);
        combo.setDifficultyCalibrated(false);
        combo.setSubmittedAt(submittedAt);
        combo.setManualReviewReason("用户重新提交，等待管理员审核");
        combo.setVideoReviewStatus("unchecked");
        combo.setVideoReviewReason("");
        combo.setVideoReviewedAt(null);
        comboMapper.markResubmitted(combo.getId(), submittedAt);
        syncFollowupParent(combo, request, currentUser);
        adminRealtimeService.publishAll("combo:resubmit", "combos", "dashboard", "characters", "userCombos", "comboDetail");
        return combo;
    }

    @Transactional
    public void deleteCombo(Long id) {
        comboFollowupMapper.delete(new LambdaQueryWrapper<ComboFollowup>()
                .eq(ComboFollowup::getParentComboId, id)
                .or()
                .eq(ComboFollowup::getFollowupComboId, id));
        comboMapper.deleteById(id);
        adminRealtimeService.publishAll("combo:delete", "combos", "dashboard", "characters", "userCombos", "favoriteCombos", "comboDetail");
    }

    public void deleteCombo(Long id, User currentUser) {
        Combo combo = combo(id);
        requireOwnerOrAdmin(combo, currentUser, "只能删除自己发布的连招");
        requireRejectedOwnerCombo(combo, currentUser, "只有被驳回的连招可以删除");
        deleteCombo(id);
    }

    public Combo approveCombo(Long id) {
        Combo combo = combo(id);
        combo.setStatus("approved");
        combo.setRejectionReason("");
        combo.setManualReviewReason("");
        combo.setDifficultyCalibrated(true);
        combo.setReviewedAt(LocalDateTime.now());
        comboMapper.updateReviewFields(combo);
        adminRealtimeService.publishAll("combo:approve", "combos", "dashboard", "characters", "userCombos", "notifications", "comboDetail");
        return combo;
    }

    public Combo reviewCombo(Long id, ComboReviewRequest request, User admin) {
        Combo combo = combo(id);
        String status = StringUtils.hasText(request.status()) ? request.status() : "approved";
        if (!List.of("approved", "rejected", "pending", "manual_review").contains(status)) {
            throw new BizException("审核状态只能是 approved/rejected/pending/manual_review");
        }
        if ("rejected".equals(status) && !StringUtils.hasText(request.rejectionReason())) {
            throw new BizException("驳回连招需要填写原因");
        }
        combo.setStatus(status);
        combo.setRejectionReason("rejected".equals(status) ? request.rejectionReason() : "");
        if (!"manual_review".equals(status)) {
            combo.setManualReviewReason("");
        }
        if (StringUtils.hasText(request.difficulty())) {
            combo.setDifficulty(request.difficulty());
        }
        combo.setDifficultyNote(request.difficultyNote());
        combo.setDifficultyCalibrated(StringUtils.hasText(request.difficulty()) || StringUtils.hasText(request.difficultyNote()));
        combo.setReviewedBy(admin == null ? "" : admin.getUsername());
        combo.setReviewedAt(LocalDateTime.now());
        comboMapper.updateReviewFields(combo);
        adminRealtimeService.publishAll("combo:review", "combos", "dashboard", "characters", "userCombos", "notifications", "comboDetail");
        return combo;
    }

    @Transactional
    public Combo updateComboVideoReview(Long id, String status, String reason, User admin) {
        combo(id);
        LocalDateTime reviewedAt = LocalDateTime.now();
        comboMapper.updateVideoReviewFields(id, status, reason, reviewedAt);
        if ("video_rejected".equals(status)) {
            comboMapper.rejectForVideo(id, admin == null ? "" : admin.getUsername(), reviewedAt);
        }
        return combo(id);
    }

    public ComboDuplicateCheckResponse duplicateCandidates(Long id) {
        return comboDuplicateService.checkExisting(id);
    }

    public ComboDuplicateCheckResponse duplicateCandidates(ComboDuplicateCheckRequest request, User viewer) {
        ComboDuplicateCheckResponse result = comboDuplicateService.check(request);
        if (viewer == null || viewer.getId() == null) {
            return new ComboDuplicateCheckResponse(result.exactDuplicate(), List.of());
        }
        return new ComboDuplicateCheckResponse(
                result.exactDuplicate(),
                result.candidates().stream()
                        .filter(candidate -> "approved".equals(candidate.combo().getStatus())
                                || viewer.getId().equals(candidate.combo().getAuthorId()))
                        .toList()
        );
    }

    private static void requireVideo(ComboRequest request) {
        if (!StringUtils.hasText(request.videoUrl())) {
            throw new BizException("上传连招必须提供演示视频");
        }
    }

    private static void validateComboCharacterSelection(ComboRequest request) {
        if (!"world-tour".equals(normalizeControlType(request.controlType()))) {
            if (request.characterId() == null) {
                throw new BizException("请选择角色");
            }
            return;
        }
        List<Long> characterIds = request.routeCharacterIds() == null ? List.of() : request.routeCharacterIds();
        long moveCount = Arrays.stream(routeText(request).split("\\s*>\\s*"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .count();
        if (moveCount == 0 || characterIds.size() != moveCount) {
            throw new BizException("环球游历连招需要为每一招选择角色");
        }
    }

    private static Long aggregateCharacterId(ComboRequest request) {
        if ("world-tour".equals(normalizeControlType(request.controlType()))
                && request.routeCharacterIds() != null
                && !request.routeCharacterIds().isEmpty()) {
            return request.routeCharacterIds().get(0);
        }
        return request.characterId();
    }

    private static List<Long> normalizedRouteCharacterIds(ComboRequest request) {
        return "world-tour".equals(normalizeControlType(request.controlType())) && request.routeCharacterIds() != null
                ? request.routeCharacterIds()
                : List.of();
    }

    private static String routeText(ComboRequest request) {
        return StringUtils.hasText(request.route()) ? request.route() : request.comboText();
    }

    @Transactional
    public ComboLikeResponse toggleComboLike(Long comboId, User currentUser) {
        Combo combo = combo(comboId);
        requireApprovedInteraction(combo);
        Long userId = currentUser.getId();
        comboLikeMapper.lockInteraction(comboId, userId);
        ComboLike existing = comboLikeMapper.selectOne(new LambdaQueryWrapper<ComboLike>()
                .eq(ComboLike::getComboId, comboId)
                .eq(ComboLike::getUserId, userId));
        boolean liked;
        if (existing == null) {
            ComboLike like = new ComboLike();
            like.setComboId(comboId);
            like.setUserId(userId);
            like.setUsername(currentUser.getUsername());
            comboLikeMapper.insert(like);
            comboMapper.adjustLikes(comboId, 1);
            liked = true;
            notifyComboInteraction(combo, currentUser.getUsername(), "combo_like", "你的连招收到了新的点赞",
                    currentUser.getUsername() + " 点赞了你的连招：" + combo.getRoute());
        } else {
            comboLikeMapper.deleteById(existing.getId());
            comboMapper.adjustLikes(comboId, -1);
            liked = false;
        }
        Integer storedLikes = combo(comboId).getLikes();
        int likes = storedLikes == null ? 0 : storedLikes;
        adminRealtimeService.publishAll("combo:like", "combos", "dashboard", "characters", "userCombos", "favoriteCombos", "comboDetail");
        return new ComboLikeResponse(comboId, liked, likes);
    }

    @Transactional
    public ComboFavoriteResponse toggleComboFavorite(Long comboId, User currentUser) {
        Combo combo = combo(comboId);
        requireApprovedInteraction(combo);
        Long userId = currentUser.getId();
        comboLikeMapper.lockInteraction(comboId, userId);
        ComboFavorite existing = comboFavoriteMapper.selectOne(new LambdaQueryWrapper<ComboFavorite>()
                .eq(ComboFavorite::getComboId, comboId)
                .eq(ComboFavorite::getUserId, userId));
        boolean favorited;
        if (existing == null) {
            ComboFavorite favorite = new ComboFavorite();
            favorite.setComboId(comboId);
            favorite.setUserId(userId);
            favorite.setUsername(currentUser.getUsername());
            comboFavoriteMapper.insert(favorite);
            comboMapper.adjustFavorites(comboId, 1);
            favorited = true;
            notifyComboInteraction(combo, currentUser.getUsername(), "combo_favorite", "你的连招被收藏了",
                    currentUser.getUsername() + " 收藏了你的连招：" + combo.getRoute());
        } else {
            comboFavoriteMapper.deleteById(existing.getId());
            comboMapper.adjustFavorites(comboId, -1);
            favorited = false;
        }
        Integer storedFavorites = combo(comboId).getFavorites();
        int favorites = storedFavorites == null ? 0 : storedFavorites;
        adminRealtimeService.publishAll("combo:favorite", "combos", "dashboard", "characters", "favoriteCombos", "comboDetail");
        return new ComboFavoriteResponse(comboId, favorited, favorites);
    }

    private void notifyComboInteraction(Combo combo, String actor, String type, String title, String content) {
        if (combo.getAuthorId() != null) {
            notificationService.notifyUserById(combo.getAuthorId(), actor, type, title, content, "/combos/" + combo.getId());
            return;
        }
        if (!StringUtils.hasText(combo.getAuthor())) {
            return;
        }
        notificationService.notifyUser(combo.getAuthor(), actor, type, title, content, "/combos/" + combo.getId());
    }

    public List<Combo> characterCombos(Long characterId) {
        return combos(null, characterId, null);
    }

    public PageResult<Combo> worldTourCombos(String starter, String tags, Integer saCost, BigDecimal driveCost,
                                             Integer damageMin, Integer damageMax, String sort,
                                             User viewer, long page, long pageSize) {
        return characterCombos(null, "world-tour", starter, tags, saCost, driveCost, damageMin, damageMax,
                sort, viewer, page, pageSize);
    }

    public ComboFilterOptions worldTourComboFilterOptions() {
        return characterComboFilterOptions(null, "world-tour");
    }

    public PageResult<Combo> characterCombos(Long characterId, String controlType, String starter, String tags,
                                             Integer saCost, BigDecimal driveCost, Integer damageMin, Integer damageMax,
                                             String sort, User viewer, long page, long pageSize) {
        List<String> normalizedTags = StringUtils.hasText(tags)
                ? Arrays.stream(tags.split(",")).map(String::trim).filter(StringUtils::hasText).distinct().limit(10).toList()
                : List.of();
        Page<Combo> result = comboMapper.selectTopLevelPage(
                Page.of(PageUtil.page(page), PageUtil.pageSize(pageSize)),
                characterId,
                StringUtils.hasText(controlType) ? normalizeControlType(controlType) : null,
                StringUtils.hasText(starter) ? uppercaseMoveText(starter) : null,
                normalizedTags, saCost, driveCost, damageMin, damageMax, sort);
        return new PageResult<>(
                withViewerState(result.getRecords(), viewer),
                result.getCurrent(),
                result.getSize(),
                result.getTotal()
        );
    }

    public ComboFilterOptions characterComboFilterOptions(Long characterId, String controlType) {
        String normalizedControlType = StringUtils.hasText(controlType) ? normalizeControlType(controlType) : null;
        List<Combo> combos = comboMapper.selectFilterOptionRows(characterId, normalizedControlType);
        List<String> starters = combos.stream()
                .map(Combo::getStarter)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> tags = combos.stream()
                .flatMap(combo -> combo.getTagList().stream())
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new ComboFilterOptions(starters, tags);
    }

    public List<Combo> parentComboOptions(Long characterId, String controlType) {
        character(characterId);
        return comboMapper.selectParentComboOptions(characterId, normalizeControlType(controlType));
    }

    public List<Combo> userCombos(Long authorId, boolean includeUnapproved) {
        return comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                .eq(Combo::getAuthorId, authorId)
                .eq(!includeUnapproved, Combo::getStatus, "approved")
                .orderByDesc(Combo::getCreatedAt)
                .orderByDesc(Combo::getId));
    }

    public List<Combo> userCombos(Long authorId, boolean includeUnapproved, User viewer) {
        return withViewerState(userCombos(authorId, includeUnapproved), viewer);
    }

    public PageResult<Combo> userCombos(Long authorId, boolean includeUnapproved, User viewer, long page, long pageSize) {
        Page<Combo> result = comboMapper.selectPage(Page.of(PageUtil.page(page), PageUtil.pageSize(pageSize)), new LambdaQueryWrapper<Combo>()
                .eq(Combo::getAuthorId, authorId)
                .eq(!includeUnapproved, Combo::getStatus, "approved")
                .orderByDesc(Combo::getCreatedAt)
                .orderByDesc(Combo::getId));
        return new PageResult<>(
                withViewerState(result.getRecords(), viewer),
                result.getCurrent(),
                result.getSize(),
                result.getTotal()
        );
    }

    public List<Combo> userFavoriteCombos(User favoriteOwner, User viewer) {
        if (favoriteOwner == null || favoriteOwner.getId() == null) {
            return List.of();
        }
        List<Long> comboIds = comboFavoriteMapper.selectList(new LambdaQueryWrapper<ComboFavorite>()
                        .eq(ComboFavorite::getUserId, favoriteOwner.getId()))
                .stream()
                .map(ComboFavorite::getComboId)
                .toList();
        if (comboIds.isEmpty()) {
            return List.of();
        }
        List<Combo> favorites = comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                .in(Combo::getId, comboIds)
                .eq(Combo::getStatus, "approved")
                .orderByDesc(Combo::getFavorites)
                .orderByDesc(Combo::getId));
        return withViewerState(favorites, viewer);
    }

    public PageResult<Combo> userFavoriteCombos(User favoriteOwner, User viewer, long page, long pageSize) {
        page = PageUtil.page(page);
        pageSize = PageUtil.pageSize(pageSize);
        if (favoriteOwner == null || favoriteOwner.getId() == null) {
            return new PageResult<>(List.of(), page, pageSize, 0);
        }
        Page<ComboFavorite> favoritePage = comboFavoriteMapper.selectPage(Page.of(page, pageSize),
                new LambdaQueryWrapper<ComboFavorite>()
                        .eq(ComboFavorite::getUserId, favoriteOwner.getId())
                        .orderByDesc(ComboFavorite::getId));
        List<Long> comboIds = favoritePage.getRecords().stream()
                .map(ComboFavorite::getComboId)
                .toList();
        if (comboIds.isEmpty()) {
            return new PageResult<>(List.of(), favoritePage.getCurrent(), favoritePage.getSize(), favoritePage.getTotal());
        }
        Map<Long, Combo> combosById = comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                        .in(Combo::getId, comboIds)
                        .eq(Combo::getStatus, "approved"))
                .stream()
                .collect(Collectors.toMap(Combo::getId, Function.identity()));
        List<Combo> ordered = comboIds.stream()
                .map(combosById::get)
                .filter(combo -> combo != null)
                .toList();
        return new PageResult<>(
                withViewerState(ordered, viewer),
                favoritePage.getCurrent(),
                favoritePage.getSize(),
                favoritePage.getTotal()
        );
    }

    private List<Combo> withViewerState(List<Combo> combos, User viewer) {
        if (combos.isEmpty()) {
            return combos;
        }
        applyFollowupParentState(combos);
        applyLatestAuthors(combos);
        combos.forEach(combo -> {
            combo.setLikes(combo.getLikes() == null ? 0 : combo.getLikes());
            combo.setFavorites(combo.getFavorites() == null ? 0 : combo.getFavorites());
            combo.setLiked(false);
            combo.setFavorited(false);
        });
        if (viewer == null || viewer.getId() == null) {
            return combos;
        }
        List<Long> comboIds = combos.stream().map(Combo::getId).toList();
        Set<Long> likedIds = comboLikeMapper.selectList(new LambdaQueryWrapper<ComboLike>()
                        .in(ComboLike::getComboId, comboIds)
                        .eq(ComboLike::getUserId, viewer.getId()))
                .stream()
                .map(ComboLike::getComboId)
                .collect(Collectors.toSet());
        Set<Long> favoritedIds = comboFavoriteMapper.selectList(new LambdaQueryWrapper<ComboFavorite>()
                        .in(ComboFavorite::getComboId, comboIds)
                        .eq(ComboFavorite::getUserId, viewer.getId()))
                .stream()
                .map(ComboFavorite::getComboId)
                .collect(Collectors.toSet());
        combos.forEach(combo -> {
            combo.setLiked(likedIds.contains(combo.getId()));
            combo.setFavorited(favoritedIds.contains(combo.getId()));
        });
        return combos;
    }

    private void applyLatestAuthors(List<Combo> combos) {
        List<Long> authorIds = combos.stream()
                .map(Combo::getAuthorId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (authorIds.isEmpty()) {
            return;
        }
        Map<Long, String> usernamesById = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, authorIds))
                .stream()
                .filter(user -> user.getId() != null && StringUtils.hasText(user.getUsername()))
                .collect(Collectors.toMap(User::getId, User::getUsername, (first, ignored) -> first));
        combos.forEach(combo -> {
            String username = usernamesById.get(combo.getAuthorId());
            if (StringUtils.hasText(username)) {
                combo.setAuthor(username);
            }
        });
    }

    private List<Combo> withoutFollowupCombos(List<Combo> combos) {
        if (combos.isEmpty()) {
            return combos;
        }
        List<Long> comboIds = combos.stream()
                .map(Combo::getId)
                .filter(id -> id != null)
                .toList();
        if (comboIds.isEmpty()) {
            return combos;
        }
        Set<Long> followupComboIds = comboFollowupMapper.selectList(new LambdaQueryWrapper<ComboFollowup>()
                        .in(ComboFollowup::getFollowupComboId, comboIds))
                .stream()
                .map(ComboFollowup::getFollowupComboId)
                .collect(Collectors.toSet());
        if (followupComboIds.isEmpty()) {
            return combos;
        }
        return combos.stream()
                .filter(combo -> !followupComboIds.contains(combo.getId()))
                .toList();
    }

    private void applyFollowupParentState(List<Combo> combos) {
        List<Long> comboIds = combos.stream()
                .map(Combo::getId)
                .filter(id -> id != null)
                .toList();
        if (comboIds.isEmpty()) {
            return;
        }
        Map<Long, Long> parentByFollowupId = comboFollowupMapper.selectList(new LambdaQueryWrapper<ComboFollowup>()
                        .in(ComboFollowup::getFollowupComboId, comboIds)
                        .orderByDesc(ComboFollowup::getCreatedAt)
                        .orderByDesc(ComboFollowup::getId))
                .stream()
                .collect(Collectors.toMap(
                        ComboFollowup::getFollowupComboId,
                        ComboFollowup::getParentComboId,
                        (first, ignored) -> first
                ));
        combos.forEach(combo -> combo.setFollowupParentId(parentByFollowupId.get(combo.getId())));
    }

    private void syncFollowupParent(Combo combo, ComboRequest request, User currentUser) {
        Long parentId = request.followupParentId();
        if (parentId == null) {
            comboFollowupMapper.delete(new LambdaQueryWrapper<ComboFollowup>()
                    .eq(ComboFollowup::getFollowupComboId, combo.getId()));
            combo.setFollowupParentId(null);
            return;
        }
        Combo parent = combo(parentId);
        if (parent.getId().equals(combo.getId())) {
            throw new BizException("后续压制连招不能关联自己");
        }
        if (!parent.getCharacterId().equals(combo.getCharacterId())) {
            throw new BizException("后续压制连招必须选择同一角色的原连招");
        }
        if (!normalizeControlType(parent.getControlType()).equals(normalizeControlType(combo.getControlType()))) {
            throw new BizException("后续压制连招必须选择同一连招类型的原连招");
        }
        if (!"approved".equals(parent.getStatus())) {
            throw new BizException("后续压制连招只能关联已审核通过的原连招");
        }
        if (comboFollowupMapper.selectCount(new LambdaQueryWrapper<ComboFollowup>()
                .eq(ComboFollowup::getFollowupComboId, parent.getId())) > 0) {
            throw new BizException("后续压制连招不能继续作为原连招");
        }
        comboFollowupMapper.upsertParent(
                parent.getId(),
                combo.getId(),
                currentUser == null ? null : currentUser.getId(),
                LocalDateTime.now()
        );
        combo.setFollowupParentId(parent.getId());
    }

    private static void apply(Combo combo, ComboRequest request) {
        String controlType = normalizeControlType(request.controlType());
        combo.setCharacterId(aggregateCharacterId(request));
        String route = uppercaseMoveText(request.route());
        combo.setStarter(uppercaseMoveText(request.starter()));
        combo.setRoute(route);
        combo.setComboText(StringUtils.hasText(request.comboText()) ? uppercaseMoveText(request.comboText()) : route);
        combo.setDamage(request.damage() == null ? 0 : request.damage());
        combo.setDriveCost(request.driveCost() == null ? BigDecimal.ZERO : request.driveCost());
        combo.setSaCost(request.saCost() == null ? 0 : request.saCost());
        combo.setAdvantageFrames(request.advantageFrames());
        combo.setDifficulty(request.difficulty());
        combo.setCornerOnly(Boolean.TRUE.equals(request.cornerOnly()));
        combo.setControlType(controlType);
        combo.setRouteCharacterIds(normalizedRouteCharacterIds(request));
        String tags = normalizeRequestedComboTags(request);
        combo.setTags(tags);
        combo.setType(firstComboTag(tags));
        combo.setVideoUrl(request.videoUrl());
        combo.setTrainingNotes(request.trainingNotes());
        combo.setDifficultyNote(request.difficultyNote());
    }

    private static void requireApprovedInteraction(Combo combo) {
        if (!"approved".equals(combo.getStatus())) {
            throw new BizException("只能点赞或收藏已审核通过的连招");
        }
    }

    private static String uppercaseMoveText(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
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

    private static boolean comboHasTag(Combo combo, String tag) {
        String target = normalizePlainText(tag);
        if (!StringUtils.hasText(target)) {
            return false;
        }
        for (String current : normalizeComboTags(combo.getTags(), combo.getType()).split(",")) {
            if (target.equals(normalizePlainText(current))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeComboTags(String tags, String fallbackType) {
        List<String> values = new ArrayList<>();
        if (StringUtils.hasText(tags)) {
            values.addAll(List.of(tags.split(",")));
        }
        return normalizeComboTags(values, fallbackType);
    }

    private static String normalizeComboTags(List<String> tags, String fallbackType) {
        List<String> values = tags == null ? List.of() : tags;
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        values.stream()
                .map(CatalogService::normalizePlainText)
                .filter(StringUtils::hasText)
                .limit(10)
                .forEach(normalized::add);
        if (normalized.isEmpty() && StringUtils.hasText(fallbackType)) {
            normalized.add(normalizePlainText(fallbackType));
        }
        if (normalized.isEmpty()) {
            normalized.add("counter-hit");
        }
        return String.join(",", normalized);
    }

    private static String normalizeRequestedComboTags(ComboRequest request) {
        if (request.followupParentId() == null) {
            String tags = normalizeComboTags(request.tags(), request.type());
            List<String> values = List.of(tags.split(","));
            if (values.stream().anyMatch(PRESSURE_TYPES::contains)) {
                throw new BizException("普通连招不能使用压制类型");
            }
            if (values.stream().anyMatch(value -> !NORMAL_COMBO_TAGS.contains(value))) {
                throw new BizException("连招包含不支持的标签");
            }
            return tags;
        }
        List<String> values = request.tags() == null ? List.of() : request.tags();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        values.stream()
                .map(CatalogService::normalizePlainText)
                .filter(StringUtils::hasText)
                .limit(10)
                .forEach(normalized::add);
        if (normalized.isEmpty() && StringUtils.hasText(request.type())) {
            normalized.add(normalizePlainText(request.type()));
        }
        List<String> types = normalized.stream().filter(PRESSURE_TYPES::contains).toList();
        if (types.size() != normalized.size()) {
            throw new BizException("压制方案不能使用普通连招标签");
        }
        if (types.size() != 1) {
            throw new BizException("压制方案必须选择一个压制类型");
        }
        return types.get(0);
    }

    private static String firstComboTag(String tags) {
        if (!StringUtils.hasText(tags)) {
            return "counter-hit";
        }
        return tags.split(",", 2)[0];
    }

    private static String normalizePlainText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
    }

    private static void requireOwnerOrAdmin(Combo combo, User currentUser, String message) {
        if (currentUser == null) {
            throw new BizException("请先登录");
        }
        boolean admin = "admin".equals(currentUser.getRole()) && "admin".equals(currentUser.getUsername());
        boolean owner = combo.getAuthorId() != null
                ? combo.getAuthorId().equals(currentUser.getId())
                : StringUtils.hasText(combo.getAuthor()) && combo.getAuthor().equals(currentUser.getUsername());
        if (!admin && !owner) {
            throw new BizException(message);
        }
    }

    private static void requireRejectedOwnerCombo(Combo combo, User currentUser, String message) {
        boolean owner = combo.getAuthorId() != null
                ? combo.getAuthorId().equals(currentUser.getId())
                : StringUtils.hasText(combo.getAuthor()) && combo.getAuthor().equals(currentUser.getUsername());
        if (!owner) {
            throw new BizException(message);
        }
        if (!"rejected".equals(combo.getStatus())) {
            throw new BizException(message);
        }
    }
}
