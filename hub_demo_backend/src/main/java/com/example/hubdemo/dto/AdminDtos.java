package com.example.hubdemo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record CharacterRequest(@Size(max = 80) String name, @Size(max = 500) String avatar,
                                   @Size(max = 1000) String description, @Size(max = 80) String archetype,
                                   @Valid CharacterDataRequest characterData) {
    }

    public record CharacterDataRequest(
            @Size(max = 40) String hp,
            @Size(max = 40) String throwRange,
            @Size(max = 40) String forwardWalkSpeed,
            @Size(max = 40) String backWalkSpeed,
            @Size(max = 40) String forwardDashSpeed,
            @Size(max = 40) String backDashSpeed,
            @Size(max = 40) String forwardDashDistance,
            @Size(max = 40) String backDashDistance,
            @Size(max = 40) String jumpSpeed,
            @Size(max = 40) String forwardJumpDistance,
            @Size(max = 40) String backJumpDistance,
            @Size(max = 40) String fastestNormal,
            @Size(max = 80) String sourceName,
            @Size(max = 500) String sourceUrl
    ) {
    }

    public record CharacterOrderRequest(@Size(max = 100) List<Long> ids) {
    }

    public record FrameRequest(
            Long characterId,
            @Size(max = 20) String controlType,
            @Size(max = 120) String moveName,
            @Size(max = 40) String startup,
            @Size(max = 40) String active,
            @Size(max = 40) String recovery,
            @Size(max = 40) String onBlock,
            @Size(max = 40) String onHit,
            @Size(max = 120) String cancel,
            @Size(max = 40) String damage,
            @Size(max = 40) String comboScaling,
            @Size(max = 40) String driveGainOnHit,
            @Size(max = 40) String driveLossOnBlock,
            @Size(max = 40) String driveLossOnPunishCounter,
            @Size(max = 40) String superArtGain,
            @Size(max = 500) String properties,
            @Size(max = 1000) String miscellaneous,
            @Size(max = 500) String sourceUrl,
            @Size(max = 80) String sourceCharacterSlug,
            @Size(max = 20) String sourceLang,
            Integer displayOrder
    ) {
    }

    public record UserAdminRequest(@Size(max = 20) String role, Boolean banned, @Size(max = 500) String banReason, LocalDateTime bannedUntil, @Size(max = 100) String adminPermissions) {
    }

    public record ReportActionRequest(@Size(max = 20) String status, @Size(max = 1000) String resolution) {
    }

    public record ReportBatchActionRequest(@Size(max = 100) List<Long> ids, @Size(max = 20) String status, @Size(max = 1000) String resolution) {
    }

    public record ReportAdminResponse(
            Long id,
            Long reporterId,
            String reporter,
            String targetType,
            Long targetId,
            String reason,
            String detail,
            String status,
            Long handlerId,
            String handler,
            String resolution,
            LocalDateTime createdAt,
            LocalDateTime handledAt,
            String targetTitle,
            String targetSubtitle,
            String targetStatus,
            String targetOwner,
            String targetUrl
    ) {
    }

    public record ComboReviewRequest(@Size(max = 20) String status, @Size(max = 500) String rejectionReason, @Size(max = 32) String difficulty, @Size(max = 500) String difficultyNote) {
    }

    public record ComboVideoReviewRequest(@Size(max = 32) String status, @Size(max = 500) String reason) {
    }

    public record AnnouncementRequest(@Size(max = 120) String title, @Size(max = 3000) String content, @Size(max = 20) String level, Boolean published) {
    }

    public record NotificationBroadcastRequest(@Size(max = 120) String title, @Size(max = 1000) String content, @Size(max = 500) String targetUrl, @Size(max = 20) String username) {
    }
}
