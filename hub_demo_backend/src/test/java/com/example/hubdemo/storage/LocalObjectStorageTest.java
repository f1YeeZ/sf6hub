package com.example.hubdemo.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalObjectStorageTest {
    @TempDir
    Path root;

    @Test
    void storesListsAndDeletesObjectsWithStablePublicUrls() throws Exception {
        LocalObjectStorage storage = new LocalObjectStorage(root.toString(), "http://localhost:8080/api");
        String key = "avatar/7/123e4567-e89b-42d3-a456-426614174000.png";

        storage.put(key, new ByteArrayInputStream(new byte[]{1, 2, 3}), 3, "image/png");

        assertThat(storage.exists(key)).isTrue();
        assertThat(storage.userUploadBytes(7L)).isEqualTo(3);
        assertThat(storage.list("avatar")).extracting(StoredObject::key).containsExactly(key);
        String url = storage.publicUrl(key);
        assertThat(url).isEqualTo("http://localhost:8080/api/uploads/" + key);
        assertThat(storage.keyFromUrl(url)).contains(key);
        assertThat(storage.keyFromUrl("/api/uploads/" + key)).contains(key);
        assertThat(storage.keyFromUrl("http://localhost:8080.evil/api/uploads/" + key)).isEmpty();

        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
    }
}
