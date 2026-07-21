package com.example.hubdemo.service;

import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.ComboMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.storage.ObjectStorage;
import com.example.hubdemo.storage.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class UploadMaintenanceService {
    private static final Logger log = LoggerFactory.getLogger(UploadMaintenanceService.class);
    private final ObjectStorage objectStorage;
    private final Duration orphanRetention;
    private final ComboMapper comboMapper;
    private final UserMapper userMapper;
    private final CharacterMapper characterMapper;

    public UploadMaintenanceService(ObjectStorage objectStorage,
                                    @Value("${app.upload-orphan-retention-hours:24}") long retentionHours,
                                    ComboMapper comboMapper, UserMapper userMapper,
                                    CharacterMapper characterMapper) {
        this.objectStorage = objectStorage;
        this.orphanRetention = Duration.ofHours(Math.max(1, retentionHours));
        this.comboMapper = comboMapper;
        this.userMapper = userMapper;
        this.characterMapper = characterMapper;
    }

    @Scheduled(cron = "${app.upload-cleanup-cron:0 30 4 * * *}")
    public void deleteExpiredOrphans() {
        Set<String> referenced = new HashSet<>();
        comboMapper.selectReferencedVideoUrls().forEach(url -> addKey(referenced, url));
        userMapper.selectReferencedAvatarUrls().forEach(url -> addKey(referenced, url));
        characterMapper.selectReferencedAvatarUrls().forEach(url -> addKey(referenced, url));
        Instant cutoff = Instant.now().minus(orphanRetention);
        for (String usage : Set.of("avatar", "combo-video")) {
            objectStorage.list(usage).forEach(object -> deleteIfOrphan(object, referenced, cutoff));
        }
    }

    private void deleteIfOrphan(StoredObject object, Set<String> referenced, Instant cutoff) {
        try {
            if (!referenced.contains(object.key()) && object.lastModified().isBefore(cutoff)) {
                objectStorage.delete(object.key());
            }
        } catch (RuntimeException exception) {
            log.warn("Unable to delete orphan upload {}", object.key(), exception);
        }
    }

    private void addKey(Set<String> referenced, String url) {
        if (!StringUtils.hasText(url)) return;
        objectStorage.keyFromUrl(url).ifPresent(referenced::add);
    }
}
