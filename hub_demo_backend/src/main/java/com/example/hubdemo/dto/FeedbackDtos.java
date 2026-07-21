package com.example.hubdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class FeedbackDtos {
    private FeedbackDtos() {
    }

    public record FeedbackRequest(
            @NotBlank(message = "请选择反馈类型")
            @Pattern(regexp = "wrong_data|abuse|duplicate|other", message = "反馈类型不合法")
            String reason,

            @NotBlank(message = "反馈内容不能为空")
            @Size(max = 500, message = "反馈内容不能超过 500 个字符")
            String detail
    ) {
    }

    public record FeedbackActionRequest(@Size(max = 20) String status, @Size(max = 1000) String resolution) {
    }

    public record FeedbackBatchActionRequest(@Size(max = 100) List<Long> ids, @Size(max = 20) String status, @Size(max = 1000) String resolution) {
    }

    public record FeedbackAdminResponse(
            Long id,
            Long userId,
            String username,
            String reason,
            String detail,
            String status,
            Long handlerId,
            String handler,
            String resolution,
            LocalDateTime createdAt,
            LocalDateTime handledAt
    ) {
    }
}
