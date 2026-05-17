package com.minitiktok.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "oauth2")
public record OAuth2ClientProperties(String clientId, String redirectUri, List<String> scopes) {

    public OAuth2ClientProperties {
        if (!StringUtils.hasText(clientId)) {
            clientId = "tiktok-web";
        }
        if (!StringUtils.hasText(redirectUri)) {
            redirectUri = "http://localhost:5173/oauth/callback";
        }
        if (scopes == null || scopes.isEmpty()) {
            scopes = List.of("video:read", "video:write", "video:like");
        } else {
            scopes = List.copyOf(scopes);
        }
    }
}
