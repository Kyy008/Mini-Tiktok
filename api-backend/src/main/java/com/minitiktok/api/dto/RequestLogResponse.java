package com.minitiktok.api.dto;

import java.time.LocalDateTime;

public record RequestLogResponse(
        Long id,
        String userId,
        String method,
        String path,
        String requestBody,
        String responseBody,
        Integer statusCode,
        Long durationMs,
        String ip,
        LocalDateTime createdAt) {
}
