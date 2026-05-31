package com.minitiktok.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.minitiktok.api.dto.InitVideoUploadRequest;
import com.minitiktok.api.dto.UploadChunkResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoUploadSessionResponse;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.entity.VideoUploadSession;
import com.minitiktok.api.mapper.VideoUploadSessionMapper;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;

@ExtendWith(MockitoExtension.class)
class VideoUploadSessionServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 22, 12, 0);

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                VideoUploadSession.class);
    }

    @Mock
    private VideoUploadSessionMapper videoUploadSessionMapper;

    @Mock
    private VideoStorageService videoStorageService;

    @Mock
    private VideoService videoService;

    @InjectMocks
    private VideoUploadSessionService videoUploadSessionService;

    @Test
    void shouldInitNewUploadSessionWithNormalizedTitleAndInitialState() {
        when(videoUploadSessionMapper.selectList(any())).thenReturn(List.of());

        VideoUploadSessionResponse response = videoUploadSessionService.initUpload(validRequest(), "uploader-1");

        assertNotNull(response.uploadId());
        assertFalse(response.uploadId().contains("-"));
        assertEquals(0, response.nextChunkIndex());
        assertEquals(0L, response.uploadedBytes());
        assertEquals(12L, response.fileSize());
        assertEquals(5, response.chunkSize());
        assertEquals(3, response.totalChunks());
        assertEquals("UPLOADING", response.status());

        ArgumentCaptor<VideoUploadSession> sessionCaptor = ArgumentCaptor.forClass(VideoUploadSession.class);
        verify(videoStorageService).validateMp4Metadata("demo.mp4", "video/mp4");
        verify(videoUploadSessionMapper).insert(sessionCaptor.capture());

        VideoUploadSession insertedSession = sessionCaptor.getValue();
        assertEquals("Demo Video", insertedSession.getTitle());
        assertEquals("uploader-1", insertedSession.getUploaderId());
        assertEquals("demo.mp4", insertedSession.getFileName());
        assertEquals("video/mp4", insertedSession.getContentType());
        assertEquals("hash123", insertedSession.getFileHash());
        assertEquals("UPLOADING", insertedSession.getStatus());
        assertNotNull(insertedSession.getCreatedAt());
        assertNotNull(insertedSession.getUpdatedAt());
    }

    @Test
    void shouldResumeExistingActiveSessionInsteadOfCreatingNewOne() {
        VideoUploadSession existingSession = uploadingSession("upload-existing", 2, 10L);
        when(videoUploadSessionMapper.selectList(any())).thenReturn(List.of(existingSession));

        VideoUploadSessionResponse response = videoUploadSessionService.initUpload(validRequest(), "uploader-1");

        assertEquals("upload-existing", response.uploadId());
        assertEquals(2, response.nextChunkIndex());
        assertEquals(10L, response.uploadedBytes());
        verify(videoUploadSessionMapper, never()).insert(any(VideoUploadSession.class));
    }

    @Test
    void shouldRejectInitWhenTotalChunksDoesNotMatchFileSizeAndChunkSize() {
        InitVideoUploadRequest request = new InitVideoUploadRequest(
                "Demo Video",
                "demo.mp4",
                12L,
                "video/mp4",
                5,
                2,
                "hash123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoUploadSessionService.initUpload(request, "uploader-1"));

        assertEquals("Total chunks does not match file size and chunk size", exception.getMessage());
        verify(videoUploadSessionMapper, never()).selectList(any());
        verify(videoUploadSessionMapper, never()).insert(any(VideoUploadSession.class));
    }

    @Test
    void shouldReturnUploadStatusForOwnedSession() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 1, 5L));

        VideoUploadSessionResponse response = videoUploadSessionService.getUploadStatus("upload-1", "uploader-1");

        assertEquals("upload-1", response.uploadId());
        assertEquals(1, response.nextChunkIndex());
        assertEquals(5L, response.uploadedBytes());
        assertEquals("UPLOADING", response.status());
    }

    @Test
    void shouldReturnNotFoundWhenSessionDoesNotBelongToUploader() {
        when(videoUploadSessionMapper.selectOne(any())).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> videoUploadSessionService.getUploadStatus("missing-upload", "uploader-1"));

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode().value());
        assertEquals("upload session not found", exception.getReason());
    }

    @Test
    void shouldAppendExpectedChunkAndAdvanceSessionState() {
        VideoUploadSession session = uploadingSession("upload-1", 1, 5L);
        byte[] chunkData = "12345".getBytes();
        when(videoUploadSessionMapper.selectOne(any())).thenReturn(session);
        when(videoStorageService.getTempUploadSize("upload-1")).thenReturn(5L);

        UploadChunkResponse response = videoUploadSessionService.uploadChunk("upload-1", 1, chunkData, "uploader-1");

        assertEquals("upload-1", response.uploadId());
        assertEquals(2, response.nextChunkIndex());
        assertEquals(10L, response.uploadedBytes());
        assertFalse(response.completed());
        verify(videoStorageService).appendChunk("upload-1", chunkData);
        verify(videoUploadSessionMapper).updateById(session);
    }

    @Test
    void shouldMarkChunkResponseCompletedAfterLastChunkArrives() {
        VideoUploadSession session = uploadingSession("upload-1", 2, 10L);
        byte[] lastChunk = "12".getBytes();
        when(videoUploadSessionMapper.selectOne(any())).thenReturn(session);
        when(videoStorageService.getTempUploadSize("upload-1")).thenReturn(10L);

        UploadChunkResponse response = videoUploadSessionService.uploadChunk("upload-1", 2, lastChunk, "uploader-1");

        assertEquals(3, response.nextChunkIndex());
        assertEquals(12L, response.uploadedBytes());
        assertTrue(response.completed());
        verify(videoStorageService).appendChunk("upload-1", lastChunk);
    }

    @Test
    void shouldReturnCurrentStateForDuplicateChunkWithoutAppendingAgain() {
        VideoUploadSession session = uploadingSession("upload-1", 2, 10L);
        when(videoUploadSessionMapper.selectOne(any())).thenReturn(session);

        UploadChunkResponse response = videoUploadSessionService.uploadChunk(
                "upload-1",
                1,
                "12345".getBytes(),
                "uploader-1");

        assertEquals(2, response.nextChunkIndex());
        assertEquals(10L, response.uploadedBytes());
        assertFalse(response.completed());
        verify(videoStorageService, never()).appendChunk(any(), any());
        verify(videoUploadSessionMapper, never()).updateById(any(VideoUploadSession.class));
    }

    @Test
    void shouldReturnConflictWhenChunkIndexIsAheadOfServerState() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 1, 5L));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> videoUploadSessionService.uploadChunk("upload-1", 2, "12345".getBytes(), "uploader-1"));

        assertEquals(HttpStatus.CONFLICT.value(), exception.getStatusCode().value());
        assertEquals("upload chunk index is ahead of server state", exception.getReason());
        verify(videoStorageService, never()).appendChunk(any(), any());
    }

    @Test
    void shouldRejectChunkWhenTemporaryFileSizeDoesNotMatchSessionState() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 1, 5L));
        when(videoStorageService.getTempUploadSize("upload-1")).thenReturn(4L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> videoUploadSessionService.uploadChunk("upload-1", 1, "12345".getBytes(), "uploader-1"));

        assertEquals("Temporary upload size does not match session state", exception.getMessage());
        verify(videoStorageService, never()).appendChunk(any(), any());
    }

    @Test
    void shouldRejectChunkWhenChunkSizeDoesNotMatchExpectedSize() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 1, 5L));
        when(videoStorageService.getTempUploadSize("upload-1")).thenReturn(5L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoUploadSessionService.uploadChunk("upload-1", 1, "1234".getBytes(), "uploader-1"));

        assertEquals("Upload chunk size does not match the expected chunk size", exception.getMessage());
        verify(videoStorageService, never()).appendChunk(any(), any());
    }

    @Test
    void shouldRejectNullChunk() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 1, 5L));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoUploadSessionService.uploadChunk("upload-1", 1, null, "uploader-1"));

        assertEquals("Upload chunk must not be empty", exception.getMessage());
        verify(videoStorageService, never()).appendChunk(any(), any());
    }

    @Test
    void shouldRejectEmptyChunk() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 1, 5L));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoUploadSessionService.uploadChunk("upload-1", 1, new byte[0], "uploader-1"));

        assertEquals("Upload chunk must not be empty", exception.getMessage());
        verify(videoStorageService, never()).appendChunk(any(), any());
    }

    @Test
    void shouldRejectNegativeChunkIndex() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 1, 5L));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoUploadSessionService.uploadChunk("upload-1", -1, "12345".getBytes(), "uploader-1"));

        assertEquals("Chunk index is out of range", exception.getMessage());
        verify(videoStorageService, never()).appendChunk(any(), any());
    }

    @Test
    void shouldRejectChunkIndexAtTotalChunksBoundary() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 1, 5L));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoUploadSessionService.uploadChunk("upload-1", 3, "12345".getBytes(), "uploader-1"));

        assertEquals("Chunk index is out of range", exception.getMessage());
        verify(videoStorageService, never()).appendChunk(any(), any());
    }

    @Test
    void shouldCompleteUploadWhenAllChunksHaveArrived() {
        VideoUploadSession session = uploadingSession("upload-1", 3, 12L);
        StoredVideoFile storedVideoFile = new StoredVideoFile(
                "hash123",
                "hash123.mp4",
                Path.of("storage/videos/hash123.mp4"),
                "video/mp4",
                12L);
        Video video = Video.builder()
                .id(7L)
                .title("Demo Video")
                .fileHash("hash123")
                .uploaderId("uploader-1")
                .deleted(false)
                .createdAt(CREATED_AT)
                .build();
        when(videoUploadSessionMapper.selectOne(any())).thenReturn(session);
        when(videoStorageService.getTempUploadSize("upload-1")).thenReturn(12L);
        when(videoStorageService.storeResumableUpload(
                eq("upload-1"),
                eq("hash123"),
                eq("demo.mp4"),
                eq("video/mp4"),
                eq(12L))).thenReturn(storedVideoFile);
        when(videoService.createUploadedVideo(eq("Demo Video"), eq("hash123"), eq("uploader-1"), any()))
                .thenReturn(video);

        UploadVideoResponse response = videoUploadSessionService.completeUpload("upload-1", "uploader-1");

        assertEquals(7L, response.id());
        assertEquals("Demo Video", response.title());
        assertEquals("/api/videos/7/play", response.playUrl());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals("COMPLETED", session.getStatus());
        verify(videoUploadSessionMapper).updateById(session);
    }

    @Test
    void shouldReturnConflictWhenCompletingIncompleteUpload() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 2, 10L));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> videoUploadSessionService.completeUpload("upload-1", "uploader-1"));

        assertEquals(HttpStatus.CONFLICT.value(), exception.getStatusCode().value());
        assertEquals("upload is not complete yet", exception.getReason());
        verify(videoStorageService, never()).storeResumableUpload(any(), any(), any(), any(), anyLong());
    }

    @Test
    void shouldRejectCompleteWhenTemporaryFileSizeDoesNotMatchSessionState() {
        when(videoUploadSessionMapper.selectOne(any()))
                .thenReturn(uploadingSession("upload-1", 3, 12L));
        when(videoStorageService.getTempUploadSize("upload-1")).thenReturn(11L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> videoUploadSessionService.completeUpload("upload-1", "uploader-1"));

        assertEquals("Temporary upload size does not match session state", exception.getMessage());
        verify(videoStorageService, never()).storeResumableUpload(any(), any(), any(), any(), anyLong());
    }

    @Test
    void shouldReturnConflictWhenSessionIsAlreadyCompleted() {
        VideoUploadSession session = uploadingSession("upload-1", 3, 12L);
        session.setStatus("COMPLETED");
        when(videoUploadSessionMapper.selectOne(any())).thenReturn(session);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> videoUploadSessionService.completeUpload("upload-1", "uploader-1"));

        assertEquals(HttpStatus.CONFLICT.value(), exception.getStatusCode().value());
        assertEquals("upload session is not accepting chunks", exception.getReason());
        verify(videoStorageService, never()).storeResumableUpload(any(), any(), any(), any(), anyLong());
    }

    private InitVideoUploadRequest validRequest() {
        return new InitVideoUploadRequest(
                "  Demo Video  ",
                "demo.mp4",
                12L,
                "video/mp4",
                5,
                3,
                "hash123");
    }

    private VideoUploadSession uploadingSession(String uploadId, int nextChunkIndex, long uploadedBytes) {
        return VideoUploadSession.builder()
                .id(1L)
                .uploadId(uploadId)
                .uploaderId("uploader-1")
                .title("Demo Video")
                .fileName("demo.mp4")
                .contentType("video/mp4")
                .fileHash("hash123")
                .fileSize(12L)
                .chunkSize(5)
                .totalChunks(3)
                .nextChunkIndex(nextChunkIndex)
                .uploadedBytes(uploadedBytes)
                .status("UPLOADING")
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }
}
