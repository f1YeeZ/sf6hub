package com.example.hubdemo.dto;

import com.example.hubdemo.entity.Combo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class ComboDtos {
    private ComboDtos() {
    }

    public record ComboRequest(
            Long characterId,
            @Size(max = 80) String starter,
            @Size(max = 1000) String route,
            @Size(max = 1000) String comboText,
            @NotNull(message = "请填写伤害") @Min(0) @Max(100000) Integer damage,
            @DecimalMin("0.0") @DecimalMax("6.0") @Digits(integer = 1, fraction = 1) BigDecimal driveCost,
            @Min(0) @Max(3) Integer saCost,
            @NotBlank(message = "请填写有利帧") @Size(max = 32) String advantageFrames,
            @NotBlank(message = "请选择连招难度") @Size(max = 32) String difficulty,
            Boolean cornerOnly,
            @Size(max = 16) String controlType,
            @Size(max = 50) List<@NotNull @Min(1) Long> routeCharacterIds,
            @Size(max = 32) String type,
            @Size(max = 10) List<@Size(max = 32) String> tags,
            @NotBlank(message = "请上传连招演示视频") @Size(max = 500) @Pattern(regexp = "^https?://.+|^/.*", message = "must be a URL or local path") String videoUrl,
            @Size(max = 1000) String trainingNotes,
            @Size(max = 500) String difficultyNote,
            @Min(1) Long followupParentId
    ) {
    }

    public record ComboLikeResponse(Long comboId, boolean liked, int likes) {
    }

    public record ComboFavoriteResponse(Long comboId, boolean favorited, int favorites) {
    }

    public record ComboFilterOptions(List<String> starters, List<String> tags) {
    }

    public record ComboDuplicateCheckRequest(
            Long characterId,
            @Size(max = 80) String starter,
            @NotBlank(message = "请填写连招路线") @Size(max = 1000) String route,
            @Size(max = 16) String controlType,
            @Size(max = 50) List<@NotNull @Min(1) Long> routeCharacterIds,
            @Size(max = 32) String type,
            @Size(max = 10) List<@Size(max = 32) String> tags,
            Boolean cornerOnly,
            @Min(1) Long followupParentId,
            @Min(1) Long excludeId
    ) {
    }

    public record ComboDuplicateCandidate(
            Combo combo,
            String matchType,
            int similarity
    ) {
    }

    public record ComboDuplicateCheckResponse(
            boolean exactDuplicate,
            List<ComboDuplicateCandidate> candidates
    ) {
    }

    public record WeeklyContributor(Long userId, String username, String avatar, long comboCount) {
    }

}
