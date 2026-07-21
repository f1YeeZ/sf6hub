package com.example.hubdemo.service;

import com.example.hubdemo.entity.FrameSyncLog;
import com.example.hubdemo.mapper.FrameSyncLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FrameSyncLogService {
    private static final String SOURCE_NAME = "Capcom Street Fighter 6";
    private static final String SOURCE_URL = "https://www.streetfighter.com/6/zh-hans/character";

    private final FrameSyncLogMapper frameSyncLogMapper;

    public FrameSyncLogService(FrameSyncLogMapper frameSyncLogMapper) {
        this.frameSyncLogMapper = frameSyncLogMapper;
    }

    public FrameSyncLog save(OfficialFrameSyncService.FrameSyncSummary summary, String prefix) {
        FrameSyncLog log = new FrameSyncLog();
        log.setSourceName(SOURCE_NAME);
        log.setSourceUrl(SOURCE_URL);
        log.setStatus(summary.successCount() == summary.totalCharacters() ? "success" : "partial");
        log.setTotalCharacters(summary.totalCharacters());
        log.setSuccessCount((int) summary.successCount());
        log.setImportedCount(summary.importedCount());
        log.setDetail(detail(summary, prefix));
        log.setCreatedAt(LocalDateTime.now());
        frameSyncLogMapper.insert(log);
        return log;
    }

    public static String detail(OfficialFrameSyncService.FrameSyncSummary summary, String prefix) {
        String base = "成功 " + summary.successCount() + "/" + summary.totalCharacters()
                + "，导入 " + summary.importedCount() + " 条";
        List<OfficialFrameSyncService.FrameSyncResult> failures = summary.results().stream()
                .filter(result -> !result.success())
                .toList();
        String detail = failures.isEmpty() ? base : base + "；失败：" + failureDetail(failures);
        return prefix == null || prefix.isBlank() ? detail : prefix + "：" + detail;
    }

    private static String failureDetail(List<OfficialFrameSyncService.FrameSyncResult> failures) {
        String failedDetail = failures.stream()
                .limit(6)
                .map(result -> result.name() + ": " + nullToEmpty(result.error()))
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
        if (failures.size() > 6) {
            failedDetail += " | 另有 " + (failures.size() - 6) + " 个角色失败";
        }
        return failedDetail;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
