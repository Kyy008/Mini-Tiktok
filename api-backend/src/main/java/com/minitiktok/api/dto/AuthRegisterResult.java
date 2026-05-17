package com.minitiktok.api.dto;

import org.springframework.http.HttpStatusCode;

public record AuthRegisterResult(HttpStatusCode status, Result<AuthUserProfileResponse> body) {
}
