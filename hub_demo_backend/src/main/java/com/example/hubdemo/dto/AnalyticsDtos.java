package com.example.hubdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record VisitTrackRequest(
            @NotBlank(message = "访问路径不能为空")
            @Size(max = 500, message = "访问路径过长")
            String path,
            @NotBlank(message = "访客标识不能为空")
            @Size(max = 64, message = "访客标识过长")
            String visitorId,
            @Size(max = 500, message = "来源页面过长")
            String referrer
    ) {}
}
