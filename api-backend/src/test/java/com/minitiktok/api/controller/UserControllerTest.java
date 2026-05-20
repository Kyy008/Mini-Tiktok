package com.minitiktok.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minitiktok.api.config.SecurityConfig;
import com.minitiktok.api.security.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CurrentUserService.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnCurrentUserWhenJwtIsPresent() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("1")
                                .claim("preferred_username", "demo")
                                .claim("scope", "video:read video:write video:like"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.userId").value("1"))
                .andExpect(jsonPath("$.data.username").value("demo"))
                .andExpect(jsonPath("$.data.scopes[0]").value("video:read"))
                .andExpect(jsonPath("$.data.scopes[1]").value("video:write"))
                .andExpect(jsonPath("$.data.scopes[2]").value("video:like"));
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 503 when auth server is unavailable during jwt decode")
    void shouldReturnServiceUnavailableWhenAuthServerIsUnavailable() throws Exception {
        when(jwtDecoder.decode("broken-token"))
                .thenThrow(new JwtException(
                        "Couldn't retrieve remote JWK set",
                        new ResourceAccessException("connect timed out")));

        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer broken-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("authentication service unavailable"));
    }
}
