package com.minitiktok.api.dto;

import java.time.LocalDateTime;

public record VideoDetailResponse(
        Long id,
        String title,
        String playUrl,
        LocalDateTime createdAt,
        String uploaderId,
        Long likeCount,
        Boolean liked) {
}
