package com.example.hubdemo.service;

import com.example.hubdemo.common.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private static final int MAX_BUCKETS = 100_000;
    private final Clock clock;
    private final ClientIpResolver clientIpResolver;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitService(ClientIpResolver clientIpResolver) {
        this(Clock.systemUTC(), clientIpResolver);
    }

    RateLimitService(Clock clock) {
        this(clock, new ClientIpResolver(""));
    }

    RateLimitService(Clock clock, ClientIpResolver clientIpResolver) {
        this.clock = clock;
        this.clientIpResolver = clientIpResolver;
    }

    public void check(HttpServletRequest request, String action, int maxAttempts, Duration window) {
        String key = action + ":" + clientIpResolver.resolve(request);
        Instant now = clock.instant();
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || !existing.windowStart().plus(window).isAfter(now)) {
                return new Bucket(now, now.plus(window), 1);
            }
            return new Bucket(existing.windowStart(), existing.expiresAt(), existing.attempts() + 1);
        });
        if (bucket.attempts() > maxAttempts) {
            throw new RateLimitException("Too many requests. Please try again later.");
        }
        if (buckets.size() > MAX_BUCKETS) {
            removeExpired(now);
            if (buckets.size() > MAX_BUCKETS) {
                buckets.remove(key);
                throw new RateLimitException("Rate limiter capacity reached. Please try again later.");
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.rate-limit-cleanup-ms:60000}")
    public void removeExpiredBuckets() {
        removeExpired(clock.instant());
    }

    private void removeExpired(Instant now) {
        buckets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    int bucketCount() {
        return buckets.size();
    }

    private record Bucket(Instant windowStart, Instant expiresAt, int attempts) {
    }
}
