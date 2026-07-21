package com.example.hubdemo.service;

import com.example.hubdemo.common.BizException;
import com.example.hubdemo.storage.ObjectStorage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UploadOwnershipService {
    private static final String COMBO_VIDEO_PREFIX = "combo-video/";
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".webm", ".mov");
    private static final Pattern UUID_FILENAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.(mp4|webm|mov)$",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectStorage objectStorage;

    public UploadOwnershipService(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    public void requireOwnedComboVideo(String videoUrl, Long userId) {
        if (userId == null || !StringUtils.hasText(videoUrl)) {
            throw invalidVideo();
        }
        String key = objectStorage.keyFromUrl(videoUrl.trim()).orElseThrow(UploadOwnershipService::invalidVideo);
        if (!key.startsWith(COMBO_VIDEO_PREFIX)) throw invalidVideo();
        String relative = key.substring(COMBO_VIDEO_PREFIX.length());
        String[] segments = relative.split("/");
        boolean legacy = segments.length == 1 && segments[0].startsWith(userId + "-");
        if (!legacy && (segments.length != 2 || !String.valueOf(userId).equals(segments[0]))) throw invalidVideo();
        String filename = (legacy ? segments[0].substring((userId + "-").length()) : segments[1])
                .toLowerCase(Locale.ROOT);
        if (!UUID_FILENAME.matcher(filename).matches() || VIDEO_EXTENSIONS.stream().noneMatch(filename::endsWith)) {
            throw invalidVideo();
        }
        if (!objectStorage.exists(key)) {
            throw invalidVideo();
        }
    }

    private static BizException invalidVideo() {
        return new BizException("连招视频必须使用本人上传且仍然存在的文件");
    }
}
