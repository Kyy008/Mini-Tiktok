package com.minitiktok.api.dto;

public record VideoUploadSessionResponse(
        String uploadId,
        int nextChunkIndex,
        long uploadedBytes,
        long fileSize,
        int chunkSize,
        int totalChunks,
        String status) {
}
