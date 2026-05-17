package com.minitiktok.auth.dto;

public record ThirdPartyVideoResponse(
        Long id,
        String ownerId,
        String title,
        String playbackUrl) {
}
