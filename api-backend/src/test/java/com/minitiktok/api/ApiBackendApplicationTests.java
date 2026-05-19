package com.minitiktok.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class ApiBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void authProxyEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/auth/login-url"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"proxy","password":"Secret123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiMeRequiresToken() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiMeReturnsCurrentJwtUser() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("7")
                                .claim("preferred_username", "demo")
                                .claim("scope", List.of("video:read", "video:write")))
                                .authorities(
                                        new SimpleGrantedAuthority("SCOPE_video:read"),
                                        new SimpleGrantedAuthority("SCOPE_video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("7"))
                .andExpect(jsonPath("$.data.username").value("demo"))
                .andExpect(jsonPath("$.data.scopes[0]").value("video:read"))
                .andExpect(jsonPath("$.data.scopes[1]").value("video:write"));
    }
}
