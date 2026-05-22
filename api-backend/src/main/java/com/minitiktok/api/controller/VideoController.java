package com.minitiktok.api.controller;

import com.minitiktok.api.dto.Result;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoDetailResponse;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoService;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class VideoController {

    private static final int MAX_TITLE_LENGTH = 128;
    private static final String VIDEO_NOT_FOUND_MESSAGE = "video not found";

    private final CurrentUserService currentUserService;
    private final VideoStorageService videoStorageService;
    private final VideoService videoService;

    @GetMapping("/api/videos/{id}")
    public ResponseEntity<Result<VideoDetailResponse>> getVideoDetail(@PathVariable("id") Long id) {
        return videoService.findActiveById(id)
                .map(video -> ResponseEntity.ok(Result.success(toVideoDetailResponse(video))))
                .orElseGet(this::videoNotFound);
    }

    @GetMapping("/api/videos/{id}/play")
    public ResponseEntity<Resource> playVideo(@PathVariable("id") Long id) {
        return videoService.findActiveById(id)
                .flatMap(video -> videoStorageService.loadAsResource(video.getFileHash()))
                .map(resource -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf("video/mp4"))
                        .body(resource))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/videos")
    public Result<UploadVideoResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) {
        String normalizedTitle = normalizeTitle(title);
        String uploaderId = currentUserService.getCurrentUser().userId();
        StoredVideoFile storedVideoFile = videoStorageService.store(file);
        LocalDateTime createdAt = LocalDateTime.now();

        Video video = videoService.createUploadedVideo(
                normalizedTitle,
                storedVideoFile.fileHash(),
                uploaderId,
                createdAt);

        UploadVideoResponse response = new UploadVideoResponse(
                video.getId(),
                video.getTitle(),
                "/api/videos/" + video.getId() + "/play",
                video.getCreatedAt());
        return Result.success(response);
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            throw new IllegalArgumentException("Video title must not be blank");
        }

        String normalizedTitle = title.trim();
        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("Video title must not be blank");
        }
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Video title must not exceed 128 characters");
        }
        return normalizedTitle;
    }

    private VideoDetailResponse toVideoDetailResponse(Video video) {
        return new VideoDetailResponse(
                video.getId(),
                video.getTitle(),
                "/api/videos/" + video.getId() + "/play",
                video.getCreatedAt(),
                video.getUploaderId());
    }

    private ResponseEntity<Result<VideoDetailResponse>> videoNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.failure(HttpStatus.NOT_FOUND.value(), VIDEO_NOT_FOUND_MESSAGE));
    }
}
