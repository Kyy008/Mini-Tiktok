package com.minitiktok.api.controller;

import com.minitiktok.api.dto.Result;
import com.minitiktok.api.dto.MyVideosPageResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoDetailResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.exception.ForbiddenVideoOperationException;
import com.minitiktok.api.exception.VideoNotFoundException;
import java.io.IOException;
import java.util.List;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.InteractionService;
import com.minitiktok.api.service.VideoService;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    private final CurrentUserService currentUserService;
    private final VideoStorageService videoStorageService;
    private final VideoService videoService;
    private final InteractionService interactionService;

    @GetMapping("/api/videos/{id}")
    public Result<VideoDetailResponse> getVideoDetail(@PathVariable("id") Long id) {
        Video video = videoService.findActiveById(id)
                .orElseThrow(VideoNotFoundException::new);
        String userId = currentUserService.getCurrentUser().userId();
        long likeCount = interactionService.getLikeCount(id);
        boolean liked = interactionService.isLikedByUser(userId, id);
        return Result.success(toVideoDetailResponse(video, likeCount, liked));
    }

    @GetMapping("/api/videos/{id}/play")
    public ResponseEntity<Resource> playVideo(@PathVariable("id") Long id) {
        return videoService.findActiveById(id)
                .flatMap(video -> videoStorageService.loadAsResource(video.getFileHash()))
                .map(this::buildPlayResponse)
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

    @DeleteMapping("/api/videos/{id}")
    public Result<Void> deleteVideo(@PathVariable("id") Long id) {
        Video video = videoService.findActiveById(id)
                .orElseThrow(VideoNotFoundException::new);
        String currentUserId = currentUserService.getCurrentUser().userId();
        if (!video.getUploaderId().equals(currentUserId)) {
            throw new ForbiddenVideoOperationException();
        }

        videoService.softDeleteById(id);
        return Result.success();
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

    private VideoDetailResponse toVideoDetailResponse(Video video, long likeCount, boolean liked) {
        return new VideoDetailResponse(
                video.getId(),
                video.getTitle(),
                "/api/videos/" + video.getId() + "/play",
                video.getCreatedAt(),
                video.getUploaderId(),
                likeCount,
                liked);
    }

    private MyVideosPageResponse.VideoItem toMyVideosItem(Video video) {
        return new MyVideosPageResponse.VideoItem(
                video.getId(),
                video.getTitle(),
                "/api/videos/" + video.getId() + "/play",
                video.getCreatedAt(),
                interactionService.getLikeCount(video.getId()));
    }

    private ResponseEntity<Resource> buildPlayResponse(Resource resource) {
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.valueOf("video/mp4"))
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read video resource metadata", ex);
        }
    }

    private void validatePagination(long page, long size) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be greater than or equal to 1");
        }
    }
}
