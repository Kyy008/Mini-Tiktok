package com.minitiktok.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minitiktok.api.config.AuthBackendProperties;
import com.minitiktok.api.config.OAuth2ClientProperties;
import com.minitiktok.api.dto.AuthLoginUrlResponse;
import com.minitiktok.api.dto.AuthRegisterResult;
import com.minitiktok.api.dto.AuthUserProfileResponse;
import com.minitiktok.api.dto.RegisterRequest;
import com.minitiktok.api.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AuthProxyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RestClient authBackendRestClient;
    private final ObjectMapper objectMapper;
    private final AuthBackendProperties authBackendProperties;
    private final OAuth2ClientProperties oauth2ClientProperties;

    /**
     * 组装授权码模式 + PKCE 的登录 URL，同时把 code_verifier 和 state 返回给前端暂存。
     */
    public AuthLoginUrlResponse createLoginUrl() {
        String codeVerifier = randomUrlSafe(32);
        String state = randomUrlSafe(32);
        String scope = String.join(" ", oauth2ClientProperties.scopes());
        String codeChallenge = codeChallenge(codeVerifier);

        URI authorizationUri = UriComponentsBuilder.fromUriString(authBackendProperties.baseUrl())
                .path("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", oauth2ClientProperties.clientId())
                .queryParam("redirect_uri", "{redirectUri}")
                .queryParam("scope", "{scope}")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode()
                .buildAndExpand(oauth2ClientProperties.redirectUri(), scope)
                .toUri();

        return new AuthLoginUrlResponse(
                authorizationUri.toString(),
                codeVerifier,
                state,
                oauth2ClientProperties.clientId(),
                oauth2ClientProperties.redirectUri(),
                scope);
    }

    /**
     * 将注册请求转发给 auth-backend，并透传后端返回的业务状态。
     */
    public AuthRegisterResult register(RegisterRequest request) {
        try {
            return authBackendRestClient.post()
                    .uri("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        Result<AuthUserProfileResponse> body = readResult(clientResponse.getBody());
                        if (body == null) {
                            body = Result.failure(
                                    clientResponse.getStatusCode().value(),
                                    clientResponse.getStatusText());
                        }
                        return new AuthRegisterResult(clientResponse.getStatusCode(), body);
                    });
        } catch (RestClientException ex) {
            return new AuthRegisterResult(
                    HttpStatus.BAD_GATEWAY,
                    Result.failure(HttpStatus.BAD_GATEWAY.value(), "auth-backend unavailable"));
        }
    }

    private Result<AuthUserProfileResponse> readResult(InputStream body) throws IOException {
        if (body == null) {
            return null;
        }
        return objectMapper.readValue(body, new TypeReference<Result<AuthUserProfileResponse>>() {
        });
    }

    /**
     * 按 OAuth2 PKCE S256 规范生成 code_challenge。
     */
    private static String codeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to create PKCE code challenge", ex);
        }
    }

    private static String randomUrlSafe(int byteCount) {
        byte[] bytes = new byte[byteCount];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
