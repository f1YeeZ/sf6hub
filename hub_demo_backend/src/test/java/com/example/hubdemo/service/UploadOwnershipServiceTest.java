package com.example.hubdemo.service;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.storage.LocalObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadOwnershipServiceTest {
    @TempDir
    Path uploadRoot;

    @Test
    void acceptsExistingVideoInCurrentUsersDirectory() throws Exception {
        String filename = "123e4567-e89b-42d3-a456-426614174000.mp4";
        Path directory = uploadRoot.resolve("combo-video").resolve("7");
        Files.createDirectories(directory);
        Files.write(directory.resolve(filename), new byte[]{0, 0, 0, 0});
        UploadOwnershipService service = new UploadOwnershipService(
                new LocalObjectStorage(uploadRoot.toString(), "https://api.example.com/api"));

        assertThatCode(() -> service.requireOwnedComboVideo(
                "https://api.example.com/api/uploads/combo-video/7/" + filename, 7L))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsExternalAndOtherUsersVideos() {
        UploadOwnershipService service = new UploadOwnershipService(
                new LocalObjectStorage(uploadRoot.toString(), "https://api.example.com/api"));

        assertThatThrownBy(() -> service.requireOwnedComboVideo("https://attacker.example/video.mp4", 7L))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.requireOwnedComboVideo(
                "//attacker.example/api/uploads/combo-video/7/123e4567-e89b-42d3-a456-426614174000.mp4", 7L))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.requireOwnedComboVideo(
                "/api/uploads/combo-video/8/123e4567-e89b-42d3-a456-426614174000.mp4", 7L))
                .isInstanceOf(BizException.class);
    }
}
