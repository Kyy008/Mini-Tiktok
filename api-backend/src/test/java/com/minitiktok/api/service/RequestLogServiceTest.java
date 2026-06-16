package com.minitiktok.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class RequestLogServiceTest {

    @Autowired
    private RequestLogService requestLogService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from request_log");
    }

    @Test
    void shouldReturnRecentLogsInDescendingIdOrder() {
        insertLog(1L, "GET", "/api/videos/recommendations", 200, 12L);
        insertLog(2L, "POST", "/api/videos/1/views", 200, 8L);
        insertLog(3L, "DELETE", "/api/videos/views", 200, 16L);

        var result = requestLogService.listRecentLogs(50, 0L);

        assertEquals(List.of(3L, 2L, 1L), result.stream().map(log -> log.id()).toList());
        assertEquals("DELETE", result.get(0).method());
        assertEquals("/api/videos/views", result.get(0).path());
        assertEquals(16L, result.get(0).durationMs());
    }

    @Test
    void shouldReturnOnlyLogsAfterIdWhenAfterIdIsProvided() {
        insertLog(1L, "GET", "/api/videos/1", 200, 5L);
        insertLog(2L, "GET", "/api/videos/2", 200, 6L);
        insertLog(3L, "GET", "/api/videos/3", 200, 7L);

        var result = requestLogService.listRecentLogs(50, 1L);

        assertEquals(List.of(3L, 2L), result.stream().map(log -> log.id()).toList());
    }

    @Test
    void shouldCapLimitAtOneHundred() {
        LongStream.rangeClosed(1, 101).forEach(id ->
                insertLog(id, "GET", "/api/videos/" + id, 200, id));

        var result = requestLogService.listRecentLogs(200, 0L);

        assertEquals(100, result.size());
        assertEquals(101L, result.get(0).id());
        assertEquals(2L, result.get(99).id());
    }

    @Test
    void shouldRejectLimitBelowOne() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> requestLogService.listRecentLogs(0, 0L));

        assertEquals("Log limit must be greater than or equal to 1", ex.getMessage());
    }

    @Test
    void shouldClearAllLogs() {
        insertLog(1L, "GET", "/api/videos/1", 200, 5L);
        insertLog(2L, "GET", "/api/videos/2", 200, 6L);

        int deleted = requestLogService.clearLogs();

        assertEquals(2, deleted);
        Integer remaining = jdbcTemplate.queryForObject("select count(*) from request_log", Integer.class);
        assertEquals(0, remaining);
    }

    private void insertLog(Long id, String method, String path, int statusCode, long durationMs) {
        jdbcTemplate.update("""
                insert into request_log
                  (id, user_id, method, path, request_body, response_body, status_code, duration_ms, ip, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "user-1",
                method,
                path,
                "[input]",
                "[output]",
                statusCode,
                durationMs,
                "127.0.0.1",
                LocalDateTime.of(2026, 6, 12, 15, 30).plusSeconds(id));
    }
}
