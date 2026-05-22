package com.minitiktok.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minitiktok.api.config.MockAuthSecurityConfig;
import com.minitiktok.api.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@ActiveProfiles("mock-auth")
@Import({MockAuthSecurityConfig.class, CurrentUserService.class})
class MockAuthModeUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissingInMockAuthMode() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUserForMockAllToken() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("local-admin"))
                .andExpect(jsonPath("$.data.username").value("local_admin"))
                .andExpect(jsonPath("$.data.scopes[0]").value("video:read"))
                .andExpect(jsonPath("$.data.scopes[1]").value("video:write"))
                .andExpect(jsonPath("$.data.scopes[2]").value("video:like"));
    }

    @Test
    void shouldReturnCurrentUserForMockVideoReadToken() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("local-reader"))
                .andExpect(jsonPath("$.data.username").value("local_reader"))
                .andExpect(jsonPath("$.data.scopes[0]").value("video:read"));
    }
}
