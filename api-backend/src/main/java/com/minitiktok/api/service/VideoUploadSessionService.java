package com.minitiktok.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minitiktok.api.dto.InitVideoUploadRequest;
import com.minitiktok.api.dto.UploadChunkResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoUploadSessionResponse;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.entity.VideoUploadSession;
import com.minitiktok.api.mapper.VideoUploadSessionMapper;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoUploadSessionService {

    private static final int MAX_TITLE_LENGTH = 128;
    private static final String STATUS_UPLOADING = "UPLOADING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final VideoUploadSessionMapper videoUploadSessionMapper;
    private final VideoStorageService videoStorageService;
    private final VideoService videoService;

    @Transactional
    public VideoUploadSessionResponse initUpload(InitVideoUploadRequest request, String uploaderId) {
        String normalizedTitle = normalizeTitle(request.title());
        validateUploadRequest(request);

        VideoUploadSession existingSession = findActiveSession(uploaderId, request.fileHash());
        if (existingSession != null) {
            return toSessionResponse(existingSession);
        }

        LocalDateTime now = LocalDateTime.now();
        VideoUploadSession session = VideoUploadSession.builder()
                .uploadId(UUID.randomUUID().toString().replace("-", ""))
                .uploaderId(uploaderId)
                .title(normalizedTitle)
                .fileName(request.fileName())
                .contentType(request.contentType())
                .fileHash(request.fileHash())
                .fileSize(request.fileSize())
                .chunkSize(request.chunkSize())
                .totalChunks(request.totalChunks())
                .nextChunkIndex(0)
                .uploadedBytes(0L)
                .status(STATUS_UPLOADING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        videoUploadSessionMapper.insert(session);
        return toSessionResponse(session);
    }

    public VideoUploadSessionResponse getUploadStatus(String uploadId, String uploaderId) {
        return toSessionResponse(requireOwnedSession(uploadId, uploaderId));
    }

    @Transactional
    public UploadChunkResponse uploadChunk(String uploadId, int chunkIndex, byte[] chunkData, String uploaderId) {
        VideoUploadSession session = requireOwnedSession(uploadId, uploaderId);
        assertUploading(session);

        if (chunkData == null || chunkData.length == 0) {
            throw new IllegalArgumentException("Upload chunk must not be empty");
        }
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new IllegalArgumentException("Chunk index is out of range");
        }
        if (chunkIndex > session.getNextChunkIndex()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "upload chunk index is ahead of server state");
        }
        if (chunkIndex < session.getNextChunkIndex()) {
            return toChunkResponse(session);
        }

        long tempUploadSize = videoStorageService.getTempUploadSize(uploadId);
        if (tempUploadSize != session.getUploadedBytes()) {
            throw new IllegalStateException("Temporary upload size does not match session state");
        }

        int expectedChunkSize = expectedChunkSize(session, chunkIndex);
        if (chunkData.length != expectedChunkSize) {
            throw new IllegalArgumentException("Upload chunk size does not match the expected chunk size");
        }

        videoStorageService.appendChunk(uploadId, chunkData);
        session.setNextChunkIndex(session.getNextChunkIndex() + 1);
        session.setUploadedBytes(session.getUploadedBytes() + chunkData.length);
        session.setUpdatedAt(LocalDateTime.now());
        videoUploadSessionMapper.updateById(session);
        return toChunkResponse(session);
    }

    @Transactional
    public UploadVideoResponse completeUpload(String uploadId, String uploaderId) {
        VideoUploadSession session = requireOwnedSession(uploadId, uploaderId);
        assertUploading(session);

        if (!session.getUploadedBytes().equals(session.getFileSize())
                || !session.getNextChunkIndex().equals(session.getTotalChunks())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "upload is not complete yet");
        }

        long tempUploadSize = videoStorageService.getTempUploadSize(uploadId);
        if (tempUploadSize != session.getUploadedBytes()) {
            throw new IllegalStateException("Temporary upload size does not match session state");
        }

        StoredVideoFile storedVideoFile = videoStorageService.storeResumableUpload(
                uploadId,
                session.getFileHash(),
                session.getFileName(),
                session.getContentType(),
                session.getFileSize());

        LocalDateTime createdAt = LocalDateTime.now();
        Video video = videoService.createUploadedVideo(
                session.getTitle(),
                storedVideoFile.fileHash(),
                uploaderId,
                createdAt);

        session.setStatus(STATUS_COMPLETED);
        session.setUpdatedAt(createdAt);
        videoUploadSessionMapper.updateById(session);

        return new UploadVideoResponse(
                video.getId(),
                video.getTitle(),
                "/api/videos/" + video.getId() + "/play",
                video.getCreatedAt());
    }

    private void validateUploadRequest(InitVideoUploadRequest request) {
        videoStorageService.validateMp4Metadata(request.fileName(), request.contentType());

        long expectedTotalChunks = (request.fileSize() + request.chunkSize() - 1L) / request.chunkSize();
        if (expectedTotalChunks != request.totalChunks()) {
            throw new IllegalArgumentException("Total chunks does not match file size and chunk size");
        }
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

    private VideoUploadSession findActiveSession(String uploaderId, String fileHash) {
        LambdaQueryWrapper<VideoUploadSession> query = new LambdaQueryWrapper<VideoUploadSession>()
                .eq(VideoUploadSession::getUploaderId, uploaderId)
                .eq(VideoUploadSession::getFileHash, fileHash)
                .eq(VideoUploadSession::getStatus, STATUS_UPLOADING)
                .orderByDesc(VideoUploadSession::getUpdatedAt)
                .last("limit 1");
        List<VideoUploadSession> sessions = videoUploadSessionMapper.selectList(query);
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    private VideoUploadSession requireOwnedSession(String uploadId, String uploaderId) {
        LambdaQueryWrapper<VideoUploadSession> query = new LambdaQueryWrapper<VideoUploadSession>()
                .eq(VideoUploadSession::getUploadId, uploadId)
                .eq(VideoUploadSession::getUploaderId, uploaderId);
        VideoUploadSession session = videoUploadSessionMapper.selectOne(query);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "upload session not found");
        }
        return session;
    }

    private void assertUploading(VideoUploadSession session) {
        if (!STATUS_UPLOADING.equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "upload session is not accepting chunks");
        }
    }

    private int expectedChunkSize(VideoUploadSession session, int chunkIndex) {
        long remainingBytes = session.getFileSize() - ((long) chunkIndex * session.getChunkSize());
        return (int) Math.min(session.getChunkSize(), remainingBytes);
    }

    private VideoUploadSessionResponse toSessionResponse(VideoUploadSession session) {
        return new VideoUploadSessionResponse(
                session.getUploadId(),
                session.getNextChunkIndex(),
                session.getUploadedBytes(),
                session.getFileSize(),
                session.getChunkSize(),
                session.getTotalChunks(),
                session.getStatus());
    }

    private UploadChunkResponse toChunkResponse(VideoUploadSession session) {
        return new UploadChunkResponse(
                session.getUploadId(),
                session.getNextChunkIndex(),
                session.getUploadedBytes(),
                session.getUploadedBytes().equals(session.getFileSize())
                        && session.getNextChunkIndex().equals(session.getTotalChunks()));
    }
}
