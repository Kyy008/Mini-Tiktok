package com.minitiktok.api.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minitiktok.api.dto.InitVideoUploadRequest;
import com.minitiktok.api.dto.Result;
import com.minitiktok.api.dto.UploadChunkResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoUploadSessionResponse;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoUploadSessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/video-uploads")
@RequiredArgsConstructor
@Tag(name = "分片上传", description = "大视频断点续传")
@SecurityRequirement(name = "BearerAuth")
public class VideoUploadController {

    private final CurrentUserService currentUserService;
    private final VideoUploadSessionService videoUploadSessionService;

    @Operation(summary = "初始化上传会话")
    @PostMapping("/init")
    public Result<VideoUploadSessionResponse> initUpload(@Valid @RequestBody InitVideoUploadRequest request) {
        String uploaderId = currentUserService.getCurrentUser().userId();
        return Result.success(videoUploadSessionService.initUpload(request, uploaderId));
    }

    @Operation(summary = "查询上传进度")
    @GetMapping("/{uploadId}")
    public Result<VideoUploadSessionResponse> getUploadStatus(@PathVariable("uploadId") String uploadId) {
        String uploaderId = currentUserService.getCurrentUser().userId();
        return Result.success(videoUploadSessionService.getUploadStatus(uploadId, uploaderId));
    }

    @Operation(summary = "上传视频分片", description = "请求体为 application/octet-stream，单片最大 10 MB")
    @PutMapping(value = "/{uploadId}/chunks/{chunkIndex}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Result<UploadChunkResponse> uploadChunk(
            @PathVariable("uploadId") String uploadId,
            @PathVariable("chunkIndex") int chunkIndex,
            HttpServletRequest request) throws IOException {
        String uploaderId = currentUserService.getCurrentUser().userId();
        byte[] chunkData = readChunkData(request);
        return Result.success(videoUploadSessionService.uploadChunk(uploadId, chunkIndex, chunkData, uploaderId));
    }

    @Operation(summary = "完成分片上传")
    @PostMapping("/{uploadId}/complete")
    public Result<UploadVideoResponse> completeUpload(@PathVariable("uploadId") String uploadId) {
        String uploaderId = currentUserService.getCurrentUser().userId();
        return Result.success(videoUploadSessionService.completeUpload(uploadId, uploaderId));
    }

    private byte[] readChunkData(HttpServletRequest request) throws IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > VideoUploadSessionService.MAX_CHUNK_SIZE_BYTES) {
            throw new IllegalArgumentException("Upload chunk size exceeds max allowed size");
        }

        int initialCapacity = contentLength > 0
                ? (int) Math.min(contentLength, VideoUploadSessionService.MAX_CHUNK_SIZE_BYTES)
                : 0;
        try (InputStream inputStream = request.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(initialCapacity)) {
            byte[] buffer = new byte[8192];
            long totalBytes = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > VideoUploadSessionService.MAX_CHUNK_SIZE_BYTES) {
                    throw new IllegalArgumentException("Upload chunk size exceeds max allowed size");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }
}
