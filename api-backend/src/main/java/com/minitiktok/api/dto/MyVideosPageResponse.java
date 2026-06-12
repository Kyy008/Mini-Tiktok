package com.minitiktok.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyVideosPageResponse(
        List<VideoItem> records,
        long page,
        long size,
        long total) {

    public record VideoItem(
            Long id,
            String title,
            String playUrl,
            LocalDateTime createdAt,
            Long likeCount,
            Long commentCount) {
    }
}
