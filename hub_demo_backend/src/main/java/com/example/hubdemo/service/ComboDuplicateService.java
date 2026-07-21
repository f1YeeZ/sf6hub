package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCandidate;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCheckRequest;
import com.example.hubdemo.dto.ComboDtos.ComboDuplicateCheckResponse;
import com.example.hubdemo.dto.ComboDtos.ComboRequest;
import com.example.hubdemo.entity.Combo;
import com.example.hubdemo.entity.ComboFollowup;
import com.example.hubdemo.mapper.ComboFollowupMapper;
import com.example.hubdemo.mapper.ComboMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComboDuplicateService {
    private static final Set<String> ACTIVE_STATUSES = Set.of("pending", "manual_review", "approved");
    private static final Set<String> IDENTITY_TAGS = Set.of(
            "counter-hit", "punish-counter", "drive-impact", "corner", "central", "anti-air", "air-hit",
            "throw-starter", "low-starter", "di-wall-splat",
            "meaty-strike", "throw-pressure", "shimmy", "safe-jump", "side-switch-pressure"
    );
    private static final int SIMILAR_ROUTE_THRESHOLD = 75;

    private final ComboMapper comboMapper;
    private final ComboFollowupMapper comboFollowupMapper;

    public ComboDuplicateService(ComboMapper comboMapper, ComboFollowupMapper comboFollowupMapper) {
        this.comboMapper = comboMapper;
        this.comboFollowupMapper = comboFollowupMapper;
    }

    public ComboDuplicateCheckResponse check(ComboDuplicateCheckRequest request) {
        ComboIdentity identity = identity(request);
        return check(identity, request.excludeId());
    }

    public ComboDuplicateCheckResponse check(ComboRequest request, Long excludeId) {
        return check(identity(request), excludeId);
    }

    public ComboDuplicateCheckResponse checkExisting(Long comboId) {
        Combo combo = comboMapper.selectById(comboId);
        if (combo == null) {
            throw new BizException("连招不存在");
        }
        applyFollowupParentState(List.of(combo));
        return check(identity(combo), comboId);
    }

    public void requireNoExactDuplicate(ComboRequest request, Long excludeId) {
        ComboDuplicateCheckResponse result = check(request, excludeId);
        if (!result.exactDuplicate()) {
            return;
        }
        Long duplicateId = result.candidates().stream()
                .filter(candidate -> "exact".equals(candidate.matchType()))
                .map(candidate -> candidate.combo().getId())
                .findFirst()
                .orElse(null);
        throw new BizException(duplicateId == null
                ? "连招重复：连招库中已存在相同路线"
                : "连招重复：已存在连招 #" + duplicateId);
    }

    public String dedupeKey(ComboRequest request) {
        return hash(identity(request).canonical());
    }

    private ComboDuplicateCheckResponse check(ComboIdentity identity, Long excludeId) {
        if (identity.characterId() == null || !StringUtils.hasText(identity.route())) {
            return new ComboDuplicateCheckResponse(false, List.of());
        }
        List<Combo> candidates = comboMapper.selectList(new LambdaQueryWrapper<Combo>()
                .eq(Combo::getCharacterId, identity.characterId())
                .ne(excludeId != null, Combo::getId, excludeId));
        applyFollowupParentState(candidates);
        List<ComboDuplicateCandidate> matches = candidates.stream()
                .filter(candidate -> normalizeControlType(candidate.getControlType()).equals(identity.controlType()))
                .map(candidate -> classify(identity, candidate))
                .filter(candidate -> candidate != null)
                .sorted(Comparator
                        .comparingInt((ComboDuplicateCandidate candidate) -> matchRank(candidate.matchType())).reversed()
                        .thenComparing(ComboDuplicateCandidate::similarity, Comparator.reverseOrder())
                        .thenComparing(candidate -> candidate.combo().getDamage() == null ? 0 : candidate.combo().getDamage(), Comparator.reverseOrder()))
                .limit(8)
                .toList();
        boolean exactDuplicate = matches.stream().anyMatch(candidate -> "exact".equals(candidate.matchType()));
        return new ComboDuplicateCheckResponse(exactDuplicate, matches);
    }

    private ComboDuplicateCandidate classify(ComboIdentity target, Combo candidate) {
        ComboIdentity current = identity(candidate);
        if (current.canonical().equals(target.canonical())) {
            return new ComboDuplicateCandidate(candidate,
                    ACTIVE_STATUSES.contains(candidate.getStatus()) ? "exact" : "historical", 100);
        }
        if (current.route().equals(target.route())) {
            return new ComboDuplicateCandidate(candidate, "same-route", 100);
        }
        if (!current.starter().equals(target.starter())) {
            return null;
        }
        int similarity = routeSimilarity(current.route(), target.route());
        return similarity >= SIMILAR_ROUTE_THRESHOLD
                ? new ComboDuplicateCandidate(candidate, "similar", similarity)
                : null;
    }

    private void applyFollowupParentState(List<Combo> combos) {
        List<Long> comboIds = combos.stream().map(Combo::getId).filter(id -> id != null).toList();
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

    private static ComboIdentity identity(ComboRequest request) {
        String controlType = normalizeControlType(request.controlType());
        List<Long> routeCharacterIds = "world-tour".equals(controlType) && request.routeCharacterIds() != null
                ? request.routeCharacterIds()
                : List.of();
        Long characterId = "world-tour".equals(controlType) && !routeCharacterIds.isEmpty()
                ? routeCharacterIds.get(0)
                : request.characterId();
        return new ComboIdentity(
                characterId,
                controlType,
                normalizeComboText(request.starter()),
                normalizeComboText(StringUtils.hasText(request.route()) ? request.route() : request.comboText()),
                routeCharacterIds,
                request.followupParentId(),
                Boolean.TRUE.equals(request.cornerOnly()),
                identityTags(request.tags(), request.type())
        );
    }

    private static ComboIdentity identity(ComboDuplicateCheckRequest request) {
        String controlType = normalizeControlType(request.controlType());
        List<Long> routeCharacterIds = "world-tour".equals(controlType) && request.routeCharacterIds() != null
                ? request.routeCharacterIds()
                : List.of();
        Long characterId = "world-tour".equals(controlType) && !routeCharacterIds.isEmpty()
                ? routeCharacterIds.get(0)
                : request.characterId();
        return new ComboIdentity(
                characterId,
                controlType,
                normalizeComboText(request.starter()),
                normalizeComboText(request.route()),
                routeCharacterIds,
                request.followupParentId(),
                Boolean.TRUE.equals(request.cornerOnly()),
                identityTags(request.tags(), request.type())
        );
    }

    private static ComboIdentity identity(Combo combo) {
        return new ComboIdentity(
                combo.getCharacterId(),
                normalizeControlType(combo.getControlType()),
                normalizeComboText(combo.getStarter()),
                normalizeComboText(StringUtils.hasText(combo.getRoute()) ? combo.getRoute() : combo.getComboText()),
                "world-tour".equals(normalizeControlType(combo.getControlType())) ? combo.getRouteCharacterIds() : List.of(),
                combo.getFollowupParentId(),
                Boolean.TRUE.equals(combo.getCornerOnly()),
                identityTags(combo.getTagList(), combo.getType())
        );
    }

    private static List<String> identityTags(List<String> tags, String fallbackType) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream()
                    .map(ComboDuplicateService::normalizePlainText)
                    .filter(IDENTITY_TAGS::contains)
                    .forEach(values::add);
        }
        String fallback = normalizePlainText(fallbackType);
        if (values.isEmpty() && IDENTITY_TAGS.contains(fallback)) {
            values.add(fallback);
        }
        return values.stream().sorted().toList();
    }

    private static int routeSimilarity(String left, String right) {
        List<String> leftMoves = routeMoves(left);
        List<String> rightMoves = routeMoves(right);
        int max = Math.max(leftMoves.size(), rightMoves.size());
        if (max == 0) {
            return 100;
        }
        return (int) Math.round((1.0 - ((double) levenshtein(leftMoves, rightMoves) / max)) * 100);
    }

    private static List<String> routeMoves(String route) {
        if (!StringUtils.hasText(route)) {
            return List.of();
        }
        return List.of(route.split(">", -1)).stream().filter(StringUtils::hasText).toList();
    }

    private static int levenshtein(List<String> left, List<String> right) {
        int[] previous = new int[right.size() + 1];
        int[] current = new int[right.size() + 1];
        for (int j = 0; j <= right.size(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.size(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.size(); j++) {
                int cost = left.get(i - 1).equals(right.get(j - 1)) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.size()];
    }

    private static String normalizeControlType(String value) {
        String normalized = normalizePlainText(value).toLowerCase(Locale.ROOT);
        if (normalized.contains("world") || normalized.contains("环球")) {
            return "world-tour";
        }
        return normalized.contains("modern") || normalized.contains("现代") || "m".equals(normalized) || "mod".equals(normalized)
                ? "modern"
                : "classic";
    }

    private static String normalizeComboText(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT);
        return normalized.replaceAll("\\s+", "").replace("＞", ">").replace("→", ">");
    }

    private static String normalizePlainText(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC).trim();
    }

    private static int matchRank(String matchType) {
        return switch (matchType) {
            case "exact" -> 4;
            case "same-route" -> 3;
            case "historical" -> 2;
            default -> 1;
        };
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 is unavailable", exception);
        }
    }

    private record ComboIdentity(
            Long characterId,
            String controlType,
            String starter,
            String route,
            List<Long> routeCharacterIds,
            Long followupParentId,
            boolean cornerOnly,
            List<String> identityTags
    ) {
        String canonical() {
            return String.join("|",
                    "v2",
                    controlType,
                    String.valueOf(characterId == null ? 0 : characterId),
                    route,
                    routeCharacterIds.stream().map(String::valueOf).collect(Collectors.joining(",")),
                    String.valueOf(followupParentId == null ? 0 : followupParentId),
                    cornerOnly ? "1" : "0",
                    String.join(",", identityTags));
        }
    }
}
