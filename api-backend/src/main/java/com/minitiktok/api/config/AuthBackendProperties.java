package com.minitiktok.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "auth-backend")
public record AuthBackendProperties(String baseUrl) {

    public AuthBackendProperties {
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "http://localhost:9000";
        }
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
