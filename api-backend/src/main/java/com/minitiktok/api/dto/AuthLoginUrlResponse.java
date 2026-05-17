package com.minitiktok.api.dto;

public record AuthLoginUrlResponse(
        String authorizationUrl,
        String codeVerifier,
        String state,
        String clientId,
        String redirectUri,
        String scope) {
}
