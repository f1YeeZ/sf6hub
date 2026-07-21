package com.example.hubdemo.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AppStartupValidator {
    private final String jwtSecret;
    private final String publicBaseUrl;
    private final String allowedOriginPatterns;
    private final String environment;
    private final String storageProvider;
    private final String ossEndpoint;
    private final String ossBucket;
    private final String ossAccessKeyId;
    private final String ossAccessKeySecret;
    private final String ossPublicBaseUrl;

    public AppStartupValidator(@Value("${app.jwt-secret}") String jwtSecret,
                               @Value("${app.public-base-url}") String publicBaseUrl,
                               @Value("${app.cors.allowed-origin-patterns}") String allowedOriginPatterns,
                               @Value("${app.environment:development}") String environment,
                               @Value("${app.storage.provider:local}") String storageProvider,
                               @Value("${app.storage.oss.endpoint:}") String ossEndpoint,
                               @Value("${app.storage.oss.bucket:}") String ossBucket,
                               @Value("${app.storage.oss.access-key-id:}") String ossAccessKeyId,
                               @Value("${app.storage.oss.access-key-secret:}") String ossAccessKeySecret,
                               @Value("${app.storage.oss.public-base-url:}") String ossPublicBaseUrl) {
        this.jwtSecret = jwtSecret;
        this.publicBaseUrl = publicBaseUrl;
        this.allowedOriginPatterns = allowedOriginPatterns;
        this.environment = environment;
        this.storageProvider = storageProvider == null ? "" : storageProvider.trim();
        this.ossEndpoint = ossEndpoint == null ? "" : ossEndpoint.trim();
        this.ossBucket = ossBucket == null ? "" : ossBucket.trim();
        this.ossAccessKeyId = ossAccessKeyId;
        this.ossAccessKeySecret = ossAccessKeySecret;
        this.ossPublicBaseUrl = ossPublicBaseUrl == null ? "" : ossPublicBaseUrl.trim();
    }

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET is required and must be at least 32 characters. Set it in your shell or IDE run configuration.");
        }
        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new IllegalStateException("PUBLIC_BASE_URL is required. Example: http://localhost:8080/api");
        }
        if (!StringUtils.hasText(allowedOriginPatterns)) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGIN_PATTERNS is required. Example: http://localhost:5173,http://127.0.0.1:5173");
        }
        if (!"local".equalsIgnoreCase(storageProvider) && !"oss".equalsIgnoreCase(storageProvider)) {
            throw new IllegalStateException("STORAGE_PROVIDER must be local or oss.");
        }
        if ("oss".equalsIgnoreCase(storageProvider)) {
            requireOssValue(ossEndpoint, "OSS_ENDPOINT");
            requireOssValue(ossBucket, "OSS_BUCKET");
            requireOssValue(ossAccessKeyId, "OSS_ACCESS_KEY_ID");
            requireOssValue(ossAccessKeySecret, "OSS_ACCESS_KEY_SECRET");
            requireOssValue(ossPublicBaseUrl, "OSS_PUBLIC_BASE_URL");
        }
        if (isPublicEnvironment()) {
            if (!publicBaseUrl.startsWith("https://")) {
                throw new IllegalStateException("PUBLIC_BASE_URL must use HTTPS in beta/production.");
            }
            for (String origin : allowedOriginPatterns.split(",")) {
                String normalized = origin.trim();
                if (!normalized.startsWith("https://") || normalized.contains("*")) {
                    throw new IllegalStateException("CORS_ALLOWED_ORIGIN_PATTERNS must contain exact HTTPS origins in beta/production.");
                }
            }
            if ("oss".equalsIgnoreCase(storageProvider)
                    && (!ossEndpoint.startsWith("https://") || !ossPublicBaseUrl.startsWith("https://"))) {
                throw new IllegalStateException("OSS_ENDPOINT and OSS_PUBLIC_BASE_URL must use HTTPS in beta/production.");
            }
        }
    }

    private static void requireOssValue(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " is required when STORAGE_PROVIDER=oss.");
        }
    }

    private boolean isPublicEnvironment() {
        return "beta".equalsIgnoreCase(environment) || "production".equalsIgnoreCase(environment)
                || "prod".equalsIgnoreCase(environment);
    }
}
