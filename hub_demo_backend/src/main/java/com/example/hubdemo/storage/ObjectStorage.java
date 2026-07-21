package com.example.hubdemo.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface ObjectStorage {
    void put(String key, InputStream input, long contentLength, String contentType) throws IOException;

    boolean exists(String key);

    void delete(String key);

    List<StoredObject> list(String prefix);

    String publicUrl(String key);

    Optional<String> keyFromUrl(String url);

    default long totalBytes(String prefix) {
        return list(prefix).stream().mapToLong(StoredObject::size).sum();
    }

    default long userUploadBytes(Long userId) {
        String owner = String.valueOf(userId);
        return totalBytes("avatar/" + owner) + totalBytes("combo-video/" + owner);
    }
}
