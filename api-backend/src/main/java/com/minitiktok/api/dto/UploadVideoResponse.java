package com.minitiktok.api.dto;

import java.time.LocalDateTime;

public record UploadVideoResponse(Long id, String title, String playUrl, LocalDateTime createdAt) {
}
