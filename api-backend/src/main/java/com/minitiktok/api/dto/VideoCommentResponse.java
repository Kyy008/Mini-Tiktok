package com.minitiktok.api.dto;

import java.time.LocalDateTime;

public record VideoCommentResponse(
        Long id,
        Long videoId,
        String userId,
        String username,
        String content,
        LocalDateTime createdAt) {
}
