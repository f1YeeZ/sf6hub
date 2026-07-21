package com.example.hubdemo.service;

import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.ComboMapper;
import com.example.hubdemo.mapper.UserMapper;
import com.example.hubdemo.storage.LocalObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadMaintenanceServiceTest {
    @TempDir
    Path uploadRoot;

    @Test
    void keepsCharacterAvatarsAndDeletesExpiredUnreferencedObjects() throws Exception {
        LocalObjectStorage storage = new LocalObjectStorage(uploadRoot.toString(), "http://localhost:8080/api");
        String referencedKey = "avatar/7/123e4567-e89b-42d3-a456-426614174000.png";
        String orphanKey = "avatar/7/223e4567-e89b-42d3-a456-426614174000.png";
        storage.put(referencedKey, new ByteArrayInputStream(new byte[]{1}), 1, "image/png");
        storage.put(orphanKey, new ByteArrayInputStream(new byte[]{2}), 1, "image/png");
        FileTime old = FileTime.from(Instant.now().minusSeconds(7200));
        Files.setLastModifiedTime(uploadRoot.resolve(referencedKey), old);
        Files.setLastModifiedTime(uploadRoot.resolve(orphanKey), old);

        ComboMapper comboMapper = mock(ComboMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        when(comboMapper.selectReferencedVideoUrls()).thenReturn(List.of());
        when(userMapper.selectReferencedAvatarUrls()).thenReturn(List.of());
        when(characterMapper.selectReferencedAvatarUrls()).thenReturn(List.of(storage.publicUrl(referencedKey)));
        UploadMaintenanceService service = new UploadMaintenanceService(
                storage, 1, comboMapper, userMapper, characterMapper
        );

        service.deleteExpiredOrphans();

        assertThat(storage.exists(referencedKey)).isTrue();
        assertThat(storage.exists(orphanKey)).isFalse();
    }
}
