package com.example.hubdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OfficialFrameSyncJob {
    private final OfficialFrameSyncService officialFrameSyncService;
    private final FrameSyncLogService frameSyncLogService;
    private final boolean enabled;
    private final boolean fullSync;

    public OfficialFrameSyncJob(OfficialFrameSyncService officialFrameSyncService,
                                FrameSyncLogService frameSyncLogService,
                                @Value("${app.official-frame-sync.enabled}") boolean enabled,
                                @Value("${app.official-frame-sync.full-sync}") boolean fullSync) {
        this.officialFrameSyncService = officialFrameSyncService;
        this.frameSyncLogService = frameSyncLogService;
        this.enabled = enabled;
        this.fullSync = fullSync;
    }

    @Scheduled(cron = "${app.official-frame-sync.cron}")
    public void syncOfficialFrames() {
        if (!enabled) {
            return;
        }
        OfficialFrameSyncService.FrameSyncSummary summary = fullSync
                ? officialFrameSyncService.syncAll()
                : officialFrameSyncService.syncNewCharacters();
        frameSyncLogService.save(summary, fullSync ? "自动全量同步" : "自动新角色同步");
    }
}
