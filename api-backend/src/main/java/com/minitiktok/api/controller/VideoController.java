package com.minitiktok.api.controller;

import com.minitiktok.api.dto.Result;
import com.minitiktok.api.dto.MyVideosPageResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoDetailResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minitiktok.api.entity.Video;
import java.util.List;
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

    @GetMapping("/api/my/videos")
    public Result<MyVideosPageResponse> getMyVideos(
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "size", defaultValue = "10") long size) {
        validatePagination(page, size);

        String uploaderId = currentUserService.getCurrentUser().userId();
        Page<Video> videoPage = videoService.pageActiveByUploaderId(uploaderId, page, size);
        List<MyVideosPageResponse.VideoItem> records = videoPage.getRecords().stream()
                .map(this::toMyVideosItem)
                .toList();

        return Result.success(new MyVideosPageResponse(
                records,
                videoPage.getCurrent(),
                videoPage.getSize(),
                videoPage.getTotal()));
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

    private MyVideosPageResponse.VideoItem toMyVideosItem(Video video) {
        return new MyVideosPageResponse.VideoItem(
                video.getId(),
                video.getTitle(),
                "/api/videos/" + video.getId() + "/play",
                video.getCreatedAt());
    }

    private void validatePagination(long page, long size) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be greater than or equal to 1");
        }
    }

    private ResponseEntity<Result<VideoDetailResponse>> videoNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.failure(HttpStatus.NOT_FOUND.value(), VIDEO_NOT_FOUND_MESSAGE));
    }
}
