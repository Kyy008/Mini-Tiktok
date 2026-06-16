package com.minitiktok.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest
class RequestLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from request_log");
    }

    @Test
    void shouldReturnRequestLogsWithoutToken() throws Exception {
        insertLog(1L, "GET", "/api/videos/recommendations", 200, 11L);

        mockMvc.perform(get("/api/request-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void shouldReturnRequestLogsWithoutVideoReadScope() throws Exception {
        insertLog(1L, "GET", "/api/videos/recommendations", 200, 11L);

        mockMvc.perform(get("/api/request-logs")
                        .with(jwt().authorities(() -> "SCOPE_video:like")
                                .jwt(jwt -> jwt.subject("user-1").claim("scope", "video:like"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldReturnRecentRequestLogsWhenUserHasVideoReadScope() throws Exception {
        insertLog(1L, "GET", "/api/videos/recommendations", 200, 11L);
        insertLog(2L, "POST", "/api/videos/1/views", 200, 18L);

        mockMvc.perform(get("/api/request-logs")
                        .param("limit", "20")
                        .param("afterId", "1")
                        .with(readJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].userId").value("user-1"))
                .andExpect(jsonPath("$.data[0].method").value("POST"))
                .andExpect(jsonPath("$.data[0].path").value("/api/videos/1/views"))
                .andExpect(jsonPath("$.data[0].statusCode").value(200))
                .andExpect(jsonPath("$.data[0].durationMs").value(18));
    }

    @Test
    void shouldRejectLimitBelowOne() throws Exception {
        mockMvc.perform(get("/api/request-logs")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Log limit must be greater than or equal to 1"));
    }

    @Test
    void shouldRejectClearingRequestLogsWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/request-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectClearingRequestLogsWithoutVideoReadScope() throws Exception {
        mockMvc.perform(delete("/api/request-logs")
                        .with(jwt().authorities(() -> "SCOPE_video:like")
                                .jwt(jwt -> jwt.subject("user-1").claim("scope", "video:like"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldClearAllRequestLogsWhenUserHasVideoReadScope() throws Exception {
        insertLog(1L, "GET", "/api/videos/1", 200, 5L);
        insertLog(2L, "GET", "/api/videos/2", 200, 6L);

        mockMvc.perform(delete("/api/request-logs")
                        .with(readJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));

        Integer remaining = jdbcTemplate.queryForObject("select count(*) from request_log", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(0, remaining);
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

    private org.springframework.test.web.servlet.request.RequestPostProcessor readJwt() {
        return jwt().authorities(() -> "SCOPE_video:read")
                .jwt(jwt -> jwt
                        .subject("user-1")
                        .claim("preferred_username", "reader")
                        .claim("scope", "video:read"));
    }
}
