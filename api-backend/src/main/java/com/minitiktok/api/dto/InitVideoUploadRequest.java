package com.minitiktok.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InitVideoUploadRequest(
        @NotBlank String title,
        @NotBlank String fileName,
        @NotNull @Min(1) Long fileSize,
        @NotBlank String contentType,
        @NotNull @Min(1) Integer chunkSize,
        @NotNull @Min(1) Integer totalChunks,
        @NotBlank String fileHash) {
}
