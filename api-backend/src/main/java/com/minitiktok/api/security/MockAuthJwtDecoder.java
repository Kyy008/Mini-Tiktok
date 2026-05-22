package com.minitiktok.api.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

public class MockAuthJwtDecoder implements JwtDecoder {

    private static final String TOKEN_VIDEO_WRITE = "mock-video-write";
    private static final String TOKEN_VIDEO_READ = "mock-video-read";
    private static final String TOKEN_VIDEO_LIKE = "mock-video-like";
    private static final String TOKEN_ALL = "mock-all";

    @Override
    public Jwt decode(String token) {
        return switch (token) {
            case TOKEN_VIDEO_WRITE -> buildJwt(token, "local-uploader", "local_uploader", "video:write");
            case TOKEN_VIDEO_READ -> buildJwt(token, "local-reader", "local_reader", "video:read");
            case TOKEN_VIDEO_LIKE -> buildJwt(token, "local-liker", "local_liker", "video:like");
            case TOKEN_ALL -> buildJwt(token, "local-admin", "local_admin", "video:read video:write video:like");
            default -> throw new BadJwtException("Unknown mock auth token");
        };
    }

    private Jwt buildJwt(String tokenValue, String subject, String username, String scope) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(3600);

        Map<String, Object> headers = Map.of(
                "alg", "none",
                "typ", "JWT");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("preferred_username", username);
        claims.put("scope", scope);

        return new Jwt(tokenValue, issuedAt, expiresAt, headers, claims);
    }
}
