package com.minitiktok.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class ApiBackendApplicationTests {

    private static HttpServer authServer;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void authBackendProperties(DynamicPropertyRegistry registry) {
        startAuthServer();
        registry.add("auth-backend.base-url",
                () -> "http://127.0.0.1:" + authServer.getAddress().getPort());
    }

    @AfterAll
    static void stopAuthServer() {
        if (authServer != null) {
            authServer.stop(0);
        }
    }

    @Test
    void contextLoads() {
    }

    @Test
    void loginUrlIsPublicAndContainsPkceParameters() throws Exception {
        mockMvc.perform(get("/api/auth/login-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorizationUrl", Matchers.containsString("/oauth2/authorize")))
                .andExpect(jsonPath("$.data.authorizationUrl", Matchers.containsString("client_id=tiktok-web")))
                .andExpect(jsonPath("$.data.authorizationUrl", Matchers.containsString("redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Foauth%2Fcallback")))
                .andExpect(jsonPath("$.data.authorizationUrl", Matchers.containsString("scope=video%3Aread%20video%3Awrite%20video%3Alike")))
                .andExpect(jsonPath("$.data.authorizationUrl", Matchers.containsString("state=")))
                .andExpect(jsonPath("$.data.authorizationUrl", Matchers.containsString("code_challenge=")))
                .andExpect(jsonPath("$.data.authorizationUrl", Matchers.containsString("code_challenge_method=S256")))
                .andExpect(jsonPath("$.data.codeVerifier").isNotEmpty())
                .andExpect(jsonPath("$.data.state").isNotEmpty())
                .andExpect(jsonPath("$.data.scope").value("video:read video:write video:like"));
    }

    @Test
    void registerProxyIsPublicAndForwardsAuthBackendResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"proxy","password":"Secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.username").value("proxy"));
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

    private static void startAuthServer() {
        if (authServer != null) {
            return;
        }
        try {
            authServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            authServer.createContext("/api/register", ApiBackendApplicationTests::handleRegister);
            authServer.start();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to start mock auth-backend", ex);
        }
    }

    private static void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, """
                    {"code":405,"message":"method not allowed"}
                    """);
            return;
        }
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        String body = new String(requestBody, StandardCharsets.UTF_8);
        if (!body.contains("\"username\":\"proxy\"")) {
            writeJson(exchange, 400, """
                    {"code":400,"message":"invalid proxy test payload"}
                    """);
            return;
        }
        writeJson(exchange, 200, """
                {"code":200,"message":"success","data":{"id":42,"username":"proxy"}}
                """);
    }

    private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
