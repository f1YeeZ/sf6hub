package com.example.hubdemo.storage;

import java.net.URI;
import java.util.Optional;

final class StorageKey {
    private StorageKey() {
    }

    static String requireSafe(String value) {
        String key = value == null ? "" : value.trim().replace('\\', '/');
        if (key.isEmpty() || key.startsWith("/") || key.endsWith("/") || key.contains("//")) {
            throw new IllegalArgumentException("Invalid object key");
        }
        for (String segment : key.split("/")) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Invalid object key");
            }
        }
        return key;
    }

    static String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    static Optional<String> fromPublicUrl(String value, String publicBaseUrl) {
        if (value == null || value.isBlank() || value.trim().startsWith("//")) return Optional.empty();
        String normalized = value.trim();
        String base = trimTrailingSlash(publicBaseUrl);
        try {
            URI source = URI.create(normalized);
            URI publicBase = URI.create(base + "/");
            if (!source.isAbsolute()) {
                String path = source.getPath();
                String marker = publicBase.getPath();
                if (path == null || marker == null || !path.startsWith(marker)) return Optional.empty();
                return optionalSafe(path.substring(marker.length()));
            }
            if (!sameOrigin(source, publicBase)) return Optional.empty();
            String basePath = publicBase.getPath();
            String path = source.getPath();
            if (path == null || !path.startsWith(basePath)) return Optional.empty();
            return optionalSafe(path.substring(basePath.length()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme() != null && left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost() != null && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static Optional<String> optionalSafe(String value) {
        try {
            return Optional.of(requireSafe(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
