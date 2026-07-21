package com.example.hubdemo.service;

import com.example.hubdemo.common.RateLimitException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminRealtimeServiceTest {
    @Test
    void limitsPublicConnectionsPerClient() {
        AdminRealtimeService service = new AdminRealtimeService(10, 1);

        service.subscribePublic("203.0.113.7");

        assertThat(service.publicConnectionCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.subscribePublic("203.0.113.7"))
                .isInstanceOf(RateLimitException.class);
    }

    @Test
    void limitsTotalPublicConnections() {
        AdminRealtimeService service = new AdminRealtimeService(1, 2);

        service.subscribePublic("203.0.113.7");

        assertThatThrownBy(() -> service.subscribePublic("203.0.113.8"))
                .isInstanceOf(RateLimitException.class);
    }
}
