package com.minitiktok.auth;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class AuthBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void jwksEndpointReturnsPublicKeySet() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray());
    }

    @Test
    void thirdPartyResourceReturnsProfileWithReadScope() throws Exception {
        mockMvc.perform(get("/third-party/resources/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("1")
                                .claim("preferred_username", "demo")
                                .claim("scope", List.of("video:read")))
                                .authorities(new SimpleGrantedAuthority("SCOPE_video:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("mini-tiktok-mock"))
                .andExpect(jsonPath("$.data.userId").value("1"))
                .andExpect(jsonPath("$.data.username").value("demo"));
    }

    @Test
    void thirdPartyWriteResourceRequiresWriteScope() throws Exception {
        mockMvc.perform(post("/third-party/resources/videos")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("1")
                                .claim("preferred_username", "demo")
                                .claim("scope", List.of("video:read")))
                                .authorities(new SimpleGrantedAuthority("SCOPE_video:read"))))
                .andExpect(status().isForbidden());
    }
}
