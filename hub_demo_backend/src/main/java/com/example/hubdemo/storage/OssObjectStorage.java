package com.example.hubdemo.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.ObjectMetadata;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "oss")
public class OssObjectStorage implements ObjectStorage {
    private final OSS client;
    private final String bucket;
    private final String publicBaseUrl;

    public OssObjectStorage(@Value("${app.storage.oss.endpoint:}") String endpoint,
                            @Value("${app.storage.oss.bucket:}") String bucket,
                            @Value("${app.storage.oss.access-key-id:}") String accessKeyId,
                            @Value("${app.storage.oss.access-key-secret:}") String accessKeySecret,
                            @Value("${app.storage.oss.security-token:}") String securityToken,
                            @Value("${app.storage.oss.public-base-url:}") String publicBaseUrl) {
        requireConfiguration(endpoint, "OSS_ENDPOINT");
        requireConfiguration(bucket, "OSS_BUCKET");
        requireConfiguration(accessKeyId, "OSS_ACCESS_KEY_ID");
        requireConfiguration(accessKeySecret, "OSS_ACCESS_KEY_SECRET");
        requireConfiguration(publicBaseUrl, "OSS_PUBLIC_BASE_URL");
        String token = securityToken == null ? "" : securityToken.trim();
        OSS ossClient = token.isEmpty()
                ? new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)
                : new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, token);
        this.client = ossClient;
        this.bucket = bucket == null ? "" : bucket.trim();
        this.publicBaseUrl = StorageKey.trimTrailingSlash(publicBaseUrl);
    }

    OssObjectStorage(OSS client, String bucket, String publicBaseUrl) {
        this.client = client;
        this.bucket = bucket;
        this.publicBaseUrl = StorageKey.trimTrailingSlash(publicBaseUrl);
    }

    @Override
    public void put(String key, InputStream input, long contentLength, String contentType) throws IOException {
        String safeKey = StorageKey.requireSafe(key);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        metadata.setContentType(contentType);
        metadata.setContentDisposition("inline");
        metadata.setCacheControl("public, max-age=31536000, immutable");
        try {
            client.putObject(bucket, safeKey, input, metadata);
        } catch (RuntimeException exception) {
            throw new StorageException("上传文件到 OSS 失败", exception);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return client.doesObjectExist(bucket, StorageKey.requireSafe(key));
        } catch (RuntimeException exception) {
            throw new StorageException("检查 OSS 文件失败", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(bucket, StorageKey.requireSafe(key));
        } catch (RuntimeException exception) {
            throw new StorageException("删除 OSS 文件失败", exception);
        }
    }

    @Override
    public List<StoredObject> list(String prefix) {
        String safePrefix = StorageKey.requireSafe(trimTrailingSlash(prefix)) + "/";
        List<StoredObject> objects = new ArrayList<>();
        String continuationToken = null;
        try {
            do {
                ListObjectsV2Request request = new ListObjectsV2Request(bucket)
                        .withPrefix(safePrefix)
                        .withMaxKeys(1000);
                request.setContinuationToken(continuationToken);
                ListObjectsV2Result result = client.listObjectsV2(request);
                result.getObjectSummaries().forEach(object -> objects.add(new StoredObject(
                        object.getKey(),
                        object.getSize(),
                        object.getLastModified().toInstant()
                )));
                continuationToken = result.isTruncated() ? result.getNextContinuationToken() : null;
            } while (continuationToken != null);
            return List.copyOf(objects);
        } catch (RuntimeException exception) {
            throw new StorageException("列举 OSS 文件失败", exception);
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

    @PreDestroy
    void shutdown() {
        client.shutdown();
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static void requireConfiguration(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " is required when STORAGE_PROVIDER=oss.");
        }
    }
}
