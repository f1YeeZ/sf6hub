package com.example.hubdemo.controller;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.RateLimitService;
import com.example.hubdemo.storage.LocalObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileControllerTest {
    @TempDir
    Path uploadRoot;

    @Test
    void rejectsUploadThatExceedsUserQuota() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        User user = new User();
        user.setId(7L);
        when(currentUserService.require(org.mockito.ArgumentMatchers.any())).thenReturn(user);
        FileController controller = new FileController(
                new LocalObjectStorage(uploadRoot.toString(), "https://api.example.com/api"), 8L,
                currentUserService, mock(RateLimitService.class)
        );
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);

        assertThatThrownBy(() -> controller.upload(file, "avatar", new MockHttpServletRequest()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("上传空间");
    }

    @Test
    void storesUploadsInsideUsersOwnDirectory() throws Exception {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        User user = new User();
        user.setId(7L);
        when(currentUserService.require(org.mockito.ArgumentMatchers.any())).thenReturn(user);
        FileController controller = new FileController(
                new LocalObjectStorage(uploadRoot.toString(), "https://api.example.com/api"), 1024L,
                currentUserService, mock(RateLimitService.class)
        );
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);

        var response = controller.upload(file, "avatar", new MockHttpServletRequest());

        assertThat(response.url()).contains("/uploads/avatar/7/");
        assertThat(Files.isRegularFile(uploadRoot.resolve("avatar").resolve("7").resolve(response.filename()))).isTrue();
    }
}
