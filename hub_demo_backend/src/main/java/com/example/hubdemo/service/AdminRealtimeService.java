package com.example.hubdemo.service;

import com.example.hubdemo.common.RateLimitException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AdminRealtimeService {
    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;
    private final CopyOnWriteArrayList<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PublicConnection> publicEmitters = new CopyOnWriteArrayList<>();
    private final AtomicLong eventId = new AtomicLong();
    private final int maxPublicConnections;
    private final int maxPublicConnectionsPerClient;

    public AdminRealtimeService(
            @Value("${app.realtime.max-public-connections:2000}") int maxPublicConnections,
            @Value("${app.realtime.max-public-connections-per-client:3}") int maxPublicConnectionsPerClient) {
        this.maxPublicConnections = Math.max(1, maxPublicConnections);
        this.maxPublicConnectionsPerClient = Math.max(1, maxPublicConnectionsPerClient);
    }

    public SseEmitter subscribe() {
        return subscribeAdmin();
    }

    public SseEmitter subscribeAdmin() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        adminEmitters.add(emitter);
        emitter.onCompletion(() -> adminEmitters.remove(emitter));
        emitter.onTimeout(() -> adminEmitters.remove(emitter));
        emitter.onError(error -> adminEmitters.remove(emitter));
        send(emitter, "admin-ready", Map.of("connectedAt", Instant.now().toString()));
        return emitter;
    }

    public SseEmitter subscribePublic(String clientKey) {
        String normalizedClientKey = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        PublicConnection connection = new PublicConnection(emitter, normalizedClientKey);
        synchronized (publicEmitters) {
            if (publicEmitters.size() >= maxPublicConnections
                    || publicEmitters.stream().filter(item -> item.clientKey().equals(normalizedClientKey)).count()
                    >= maxPublicConnectionsPerClient) {
                throw new RateLimitException("Too many realtime connections. Please close another tab and try again.");
            }
            publicEmitters.add(connection);
        }
        emitter.onCompletion(() -> publicEmitters.remove(connection));
        emitter.onTimeout(() -> publicEmitters.remove(connection));
        emitter.onError(error -> publicEmitters.remove(connection));
        if (!send(emitter, "realtime-ready", Map.of("connectedAt", Instant.now().toString()))) {
            publicEmitters.remove(connection);
        }
        return emitter;
    }

    public void publish(String action, String... areas) {
        publishAdmin(action, areas);
    }

    public void publishAdmin(String action, String... areas) {
        publishTo(adminEmitters, "admin-update", action, areas);
    }

    public void publishPublic(String action, String... areas) {
        publishTo(publicEmitters.stream().map(PublicConnection::emitter).toList(),
                "realtime-update", action, areas);
    }

    public void publishAll(String action, String... areas) {
        publishAdmin(action, areas);
        publishPublic(action, areas);
    }

    private void publishTo(List<SseEmitter> emitters, String eventName, String action, String... areas) {
        if (emitters.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", eventId.incrementAndGet());
        payload.put("action", action);
        payload.put("areas", Arrays.stream(areas).distinct().toList());
        payload.put("emittedAt", Instant.now().toString());
        for (SseEmitter emitter : List.copyOf(emitters)) {
            if (!send(emitter, eventName, payload)) {
                adminEmitters.remove(emitter);
                publicEmitters.removeIf(item -> item.emitter() == emitter);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.realtime.heartbeat-ms:25000}")
    public void heartbeat() {
        adminEmitters.removeIf(emitter -> !sendHeartbeat(emitter));
        publicEmitters.removeIf(connection -> !sendHeartbeat(connection.emitter()));
    }

    int publicConnectionCount() {
        return publicEmitters.size();
    }

    private boolean send(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload)
                    .reconnectTime(2000L));
            return true;
        } catch (IOException | IllegalStateException exception) {
            return false;
        }
    }

    private boolean sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("keepalive"));
            return true;
        } catch (IOException | IllegalStateException exception) {
            return false;
        }
    }

    private record PublicConnection(SseEmitter emitter, String clientKey) {
    }
}
