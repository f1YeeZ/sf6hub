package com.example.hubdemo.controller;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.dto.FileDtos.FileResponse;
import com.example.hubdemo.entity.User;
import com.example.hubdemo.service.CurrentUserService;
import com.example.hubdemo.service.RateLimitService;
import com.example.hubdemo.storage.ObjectStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
public class FileController {
    private static final Map<String, UploadPolicy> POLICIES = Map.of(
            "avatar", new UploadPolicy(
                    5 * 1024 * 1024L,
                    Map.of(
                            "image/jpeg", ".jpg",
                            "image/png", ".png",
                            "image/webp", ".webp",
                            "image/gif", ".gif"
                    )
            ),
            "combo-video", new UploadPolicy(
                    100 * 1024 * 1024L,
                    Map.of(
                            "video/mp4", ".mp4",
                            "video/webm", ".webm",
                            "video/quicktime", ".mov"
                    )
            )
    );

    private final ObjectStorage objectStorage;
    private final CurrentUserService currentUserService;
    private final RateLimitService rateLimitService;
    private final long userQuotaBytes;
    private final Object[] uploadLocks = new Object[64];

    public FileController(ObjectStorage objectStorage,
                           @Value("${app.upload-user-quota-bytes:1073741824}") long userQuotaBytes,
                           CurrentUserService currentUserService,
                           RateLimitService rateLimitService) {
        this.objectStorage = objectStorage;
        this.userQuotaBytes = userQuotaBytes;
        this.currentUserService = currentUserService;
        this.rateLimitService = rateLimitService;
        Arrays.setAll(uploadLocks, ignored -> new Object());
    }

    @PostMapping("/files")
    public FileResponse upload(@RequestParam MultipartFile file,
                               @RequestParam(defaultValue = "avatar") String usage,
                               HttpServletRequest request) throws Exception {
        rateLimitService.check(request, "files:upload", 30, Duration.ofMinutes(10));
        User user = currentUserService.require(request);
        if (file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        UploadPolicy policy = POLICIES.get(usage);
        if (policy == null) {
            throw new BizException("usage 只能是 avatar/combo-video");
        }
        if (file.getSize() > policy.maxBytes()) {
            throw new BizException("文件大小超出限制");
        }
        String contentType = normalizeContentType(file.getContentType());
        String extension = policy.extensionByContentType().get(contentType);
        if (extension == null) {
            throw new BizException("文件类型不支持");
        }
        String originalExtension = extensionOf(file.getOriginalFilename());
        if (StringUtils.hasText(originalExtension) && !extension.equals(originalExtension)) {
            throw new BizException("文件扩展名与类型不匹配");
        }
        validateFileSignature(file, contentType);
        String filename = UUID.randomUUID() + extension;
        String objectKey = usage + "/" + user.getId() + "/" + filename;
        synchronized (uploadLocks[Math.floorMod(user.getId().hashCode(), uploadLocks.length)]) {
            long usedBytes = objectStorage.userUploadBytes(user.getId());
            if (usedBytes + file.getSize() > userQuotaBytes) {
                throw new BizException("个人上传空间已达到上限，请删除旧文件后重试");
            }
            try (var input = file.getInputStream()) {
                objectStorage.put(objectKey, input, file.getSize(), contentType);
            }
        }
        String url = objectStorage.publicUrl(objectKey);
        return new FileResponse(url, filename, contentType, usage);
    }

    private static String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private static String extensionOf(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        String cleaned = StringUtils.cleanPath(filename).toLowerCase(Locale.ROOT);
        int index = cleaned.lastIndexOf('.');
        return index >= 0 ? cleaned.substring(index) : "";
    }

    private static void validateFileSignature(MultipartFile file, String contentType) throws IOException {
        byte[] header = file.getInputStream().readNBytes(16);
        boolean valid = switch (contentType) {
            case "image/jpeg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/gif" -> startsWith(header, "GIF87a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                    || startsWith(header, "GIF89a".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            case "image/webp" -> startsWith(header, "RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                    && bytesAt(header, 8, "WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            case "video/mp4", "video/quicktime" -> bytesAt(header, 4, "ftyp".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            case "video/webm" -> startsWith(header, 0x1A, 0x45, 0xDF, 0xA3);
            default -> false;
        };
        if (!valid) {
            throw new BizException("文件内容与类型不匹配");
        }
    }

    private static boolean startsWith(byte[] value, int... expected) {
        if (value.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((value[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] value, byte[] expected) {
        return bytesAt(value, 0, expected);
    }

    private static boolean bytesAt(byte[] value, int offset, byte[] expected) {
        return value.length >= offset + expected.length
                && Arrays.equals(Arrays.copyOfRange(value, offset, offset + expected.length), expected);
    }

    private record UploadPolicy(long maxBytes, Map<String, String> extensionByContentType) {
    }
}
