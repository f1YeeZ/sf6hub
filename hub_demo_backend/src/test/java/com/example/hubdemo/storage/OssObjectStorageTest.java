package com.example.hubdemo.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectMetadata;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OssObjectStorageTest {
    @Test
    void createsSdkClientWithConfiguredCredentialsWithoutMakingNetworkCalls() {
        OssObjectStorage storage = new OssObjectStorage(
                "https://oss-cn-shanghai.aliyuncs.com",
                "hub-bucket",
                "fake-access-key-id",
                "fake-access-key-secret",
                "",
                "https://cdn.example.com"
        );

        storage.shutdown();
    }

    @Test
    void writesAndListsObjectsWithoutContactingRealOss() throws Exception {
        OSS client = mock(OSS.class);
        OssObjectStorage storage = new OssObjectStorage(client, "hub-bucket", "https://cdn.example.com/media");
        String key = "combo-video/7/123e4567-e89b-42d3-a456-426614174000.mp4";
        OSSObjectSummary summary = new OSSObjectSummary();
        summary.setKey(key);
        summary.setSize(4);
        summary.setLastModified(Date.from(Instant.parse("2026-07-15T00:00:00Z")));
        ListObjectsV2Result result = mock(ListObjectsV2Result.class);
        when(result.getObjectSummaries()).thenReturn(List.of(summary));
        when(result.isTruncated()).thenReturn(false);
        when(client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(result);
        when(client.doesObjectExist("hub-bucket", key)).thenReturn(true);

        storage.put(key, new ByteArrayInputStream(new byte[]{0, 0, 0, 0}), 4, "video/mp4");

        verify(client).putObject(eq("hub-bucket"), eq(key), any(ByteArrayInputStream.class), any(ObjectMetadata.class));
        assertThat(storage.exists(key)).isTrue();
        assertThat(storage.list("combo-video/7")).containsExactly(
                new StoredObject(key, 4, Instant.parse("2026-07-15T00:00:00Z"))
        );
        assertThat(storage.publicUrl(key)).isEqualTo("https://cdn.example.com/media/" + key);
        assertThat(storage.keyFromUrl("https://cdn.example.com/media/" + key)).contains(key);
        assertThat(storage.keyFromUrl("https://cdn.example.com.evil/media/" + key)).isEmpty();
    }
}
