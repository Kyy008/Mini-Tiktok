package com.minitiktok.api.dto;

public record UploadChunkResponse(
        String uploadId,
        int nextChunkIndex,
        long uploadedBytes,
        boolean completed) {
}
