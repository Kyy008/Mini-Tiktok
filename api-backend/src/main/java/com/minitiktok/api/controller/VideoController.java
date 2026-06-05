package com.minitiktok.api.controller;

import com.minitiktok.api.dto.Result;
import com.minitiktok.api.dto.MyVideosPageResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoDetailResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.exception.ForbiddenVideoOperationException;
import com.minitiktok.api.exception.VideoNotFoundException;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.security.CurrentUser;
import com.minitiktok.api.service.InteractionService;
import com.minitiktok.api.service.VideoService;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
        long likeCount = interactionService.getLikeCount(id);
        Optional<CurrentUser> currentUser = currentUserService.findCurrentUser();
        boolean liked = currentUser
                .map(user -> interactionService.isLikedByUser(user.userId(), id))
                .orElse(false);
        return Result.success(toVideoDetailResponse(video, likeCount, liked));
    }

    @GetMapping("/api/videos/{id}/play")
    public ResponseEntity<?> playVideo(
            @PathVariable("id") Long id,
            @RequestHeader HttpHeaders requestHeaders) {
        return videoService.findActiveById(id)
                .flatMap(video -> videoStorageService.loadAsResource(video.getFileHash()))
                .map(resource -> buildPlayResponse(resource, requestHeaders))
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

    private ResponseEntity<?> buildPlayResponse(Resource resource, HttpHeaders requestHeaders) {
        try {
            String rangeHeader = requestHeaders.getFirst(HttpHeaders.RANGE);
            if (rangeHeader != null && !rangeHeader.isBlank()) {
                return buildPartialPlayResponse(resource, rangeHeader);
            }

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

    private ResponseEntity<?> buildPartialPlayResponse(Resource resource, String rangeHeader) throws IOException {
        long contentLength = resource.contentLength();
        RangeBounds range = parseRange(rangeHeader, contentLength);
        if (range == null) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.valueOf("video/mp4"))
                    .build();
        }

        long rangeLength = range.endInclusive() - range.start() + 1;
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes %d-%d/%d".formatted(range.start(), range.endInclusive(), contentLength))
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.valueOf("video/mp4"))
                .contentLength(rangeLength)
                .body(new InputStreamResource(openRangeInputStream(resource, range, rangeLength)));
    }

    private InputStream openRangeInputStream(Resource resource, RangeBounds range, long rangeLength) throws IOException {
        InputStream inputStream = resource.getInputStream();
        try {
            inputStream.skipNBytes(range.start());
            return new LimitedInputStream(inputStream, rangeLength);
        } catch (IOException | RuntimeException ex) {
            inputStream.close();
            throw ex;
        }
    }

    private RangeBounds parseRange(String rangeHeader, long contentLength) {
        if (contentLength <= 0 || !rangeHeader.startsWith("bytes=")) {
            return null;
        }

        String rangeSpec = rangeHeader.substring("bytes=".length()).trim();
        if (rangeSpec.contains(",")) {
            rangeSpec = rangeSpec.substring(0, rangeSpec.indexOf(',')).trim();
        }

        int separatorIndex = rangeSpec.indexOf('-');
        if (separatorIndex < 0) {
            return null;
        }

        try {
            String startPart = rangeSpec.substring(0, separatorIndex).trim();
            String endPart = rangeSpec.substring(separatorIndex + 1).trim();
            long start;
            long endInclusive;

            if (startPart.isEmpty()) {
                long suffixLength = Long.parseLong(endPart);
                if (suffixLength <= 0) {
                    return null;
                }
                start = Math.max(contentLength - suffixLength, 0);
                endInclusive = contentLength - 1;
            } else {
                start = Long.parseLong(startPart);
                endInclusive = endPart.isEmpty()
                        ? contentLength - 1
                        : Math.min(Long.parseLong(endPart), contentLength - 1);
            }

            if (start < 0 || start >= contentLength || endInclusive < start) {
                return null;
            }
            return new RangeBounds(start, endInclusive);
        } catch (NumberFormatException ex) {
            return null;
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

    private record RangeBounds(long start, long endInclusive) {
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private long remainingBytes;

        private LimitedInputStream(InputStream inputStream, long remainingBytes) {
            super(inputStream);
            this.remainingBytes = remainingBytes;
        }

        @Override
        public int read() throws IOException {
            if (remainingBytes <= 0) {
                return -1;
            }

            int readByte = super.read();
            if (readByte != -1) {
                remainingBytes--;
            }
            return readByte;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remainingBytes <= 0) {
                return -1;
            }

            int bytesRead = super.read(buffer, offset, (int) Math.min(length, remainingBytes));
            if (bytesRead != -1) {
                remainingBytes -= bytesRead;
            }
            return bytesRead;
        }
    }
}
