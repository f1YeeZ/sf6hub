package com.example.hubdemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthControllerTest {
    @Test
    void returnsUpWhenDatabaseProbeSucceeds() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        ResponseEntity<Map<String, Object>> response = new HealthController(jdbcTemplate).health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> health = response.getBody();
        assertThat(health).containsEntry("status", "UP");
        assertThat(health).containsEntry("database", "UP");
        assertThat(health.get("timestamp")).isInstanceOf(String.class);
        verify(jdbcTemplate).queryForObject("SELECT 1", Integer.class);
    }

    @Test
    void returnsServiceUnavailableWhenDatabaseProbeFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenThrow(new IllegalStateException("down"));

        ResponseEntity<Map<String, Object>> response = new HealthController(jdbcTemplate).health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", "DOWN");
        assertThat(response.getBody()).containsEntry("database", "DOWN");
    }
}
