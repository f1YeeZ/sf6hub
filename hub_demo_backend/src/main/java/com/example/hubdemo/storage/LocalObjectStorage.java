package com.example.hubdemo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStorage {
    private final Path root;
    private final String publicBaseUrl;

    public LocalObjectStorage(@Value("${app.upload-dir}") String uploadDir,
                              @Value("${app.public-base-url}") String apiPublicBaseUrl) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = StorageKey.trimTrailingSlash(apiPublicBaseUrl) + "/uploads";
    }

    @Override
    public void put(String key, InputStream input, long contentLength, String contentType) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new StorageException("删除本地上传文件失败", exception);
        }
    }

    @Override
    public List<StoredObject> list(String prefix) {
        String safePrefix = normalizePrefix(prefix);
        Path directory = root.resolve(safePrefix).normalize();
        if (!directory.startsWith(root) || !Files.isDirectory(directory)) return List.of();
        try (var paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> new StoredObject(
                            root.relativize(path).toString().replace('\\', '/'),
                            fileSize(path),
                            lastModified(path)
                    ))
                    .toList();
        } catch (IOException exception) {
            throw new StorageException("扫描本地上传目录失败", exception);
        }
    }

    @Override
    public String publicUrl(String key) {
        return publicBaseUrl + "/" + StorageKey.requireSafe(key);
    }

    @Override
    public Optional<String> keyFromUrl(String url) {
        return StorageKey.fromPublicUrl(url, publicBaseUrl);
    }

    @Override
    public long userUploadBytes(Long userId) {
        long total = ObjectStorage.super.userUploadBytes(userId);
        String legacyPrefix = userId + "-";
        for (String usage : List.of("avatar", "combo-video")) {
            Path usageDirectory = root.resolve(usage).normalize();
            if (!usageDirectory.startsWith(root) || !Files.isDirectory(usageDirectory)) continue;
            try (var paths = Files.list(usageDirectory)) {
                total += paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith(legacyPrefix))
                        .mapToLong(LocalObjectStorage::fileSize)
                        .sum();
            } catch (IOException exception) {
                throw new StorageException("统计本地上传配额失败", exception);
            }
        }
        return total;
    }

    Path root() {
        return root;
    }

    private Path resolve(String key) {
        Path target = root.resolve(StorageKey.requireSafe(key)).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid object key");
        return target;
    }

    private static String normalizePrefix(String prefix) {
        String value = prefix == null ? "" : prefix.trim().replace('\\', '/');
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return StorageKey.requireSafe(value);
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static java.time.Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException exception) {
            return java.time.Instant.EPOCH;
        }
    }
}
