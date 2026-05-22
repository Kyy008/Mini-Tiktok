package com.minitiktok.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;

import com.minitiktok.api.config.SecurityConfig;
import com.minitiktok.api.dto.UploadChunkResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoUploadSessionResponse;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoUploadSessionService;

@WebMvcTest(VideoUploadController.class)
@Import({SecurityConfig.class, CurrentUserService.class})
class VideoUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoUploadSessionService videoUploadSessionService;

    @Test
    void shouldInitUploadSessionWhenUserHasVideoWriteScope() throws Exception {
        when(videoUploadSessionService.initUpload(any(), eq("1")))
                .thenReturn(new VideoUploadSessionResponse("upload123", 0, 0L, 12L, 5, 3, "UPLOADING"));

        mockMvc.perform(post("/api/video-uploads/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Demo Video",
                                  "fileName": "demo.mp4",
                                  "fileSize": 12,
                                  "contentType": "video/mp4",
                                  "chunkSize": 5,
                                  "totalChunks": 3,
                                  "fileHash": "hash123"
                                }
                                """)
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt.subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.uploadId").value("upload123"))
                .andExpect(jsonPath("$.data.nextChunkIndex").value(0))
                .andExpect(jsonPath("$.data.uploadedBytes").value(0))
                .andExpect(jsonPath("$.data.totalChunks").value(3))
                .andExpect(jsonPath("$.data.status").value("UPLOADING"));
    }

    @Test
    void shouldReturnUploadStatusWhenUserHasVideoWriteScope() throws Exception {
        when(videoUploadSessionService.getUploadStatus("upload123", "1"))
                .thenReturn(new VideoUploadSessionResponse("upload123", 2, 10L, 12L, 5, 3, "UPLOADING"));

        mockMvc.perform(get("/api/video-uploads/upload123")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt.subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextChunkIndex").value(2))
                .andExpect(jsonPath("$.data.uploadedBytes").value(10));
    }

    @Test
    void shouldUploadChunkWhenChunkOrderMatchesServerState() throws Exception {
        when(videoUploadSessionService.uploadChunk(eq("upload123"), eq(0), any(), eq("1")))
                .thenReturn(new UploadChunkResponse("upload123", 1, 5L, false));

        mockMvc.perform(put("/api/video-uploads/upload123/chunks/0")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("12345".getBytes())
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt.subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadId").value("upload123"))
                .andExpect(jsonPath("$.data.nextChunkIndex").value(1))
                .andExpect(jsonPath("$.data.uploadedBytes").value(5))
                .andExpect(jsonPath("$.data.completed").value(false));
    }

    @Test
    void shouldReturnConflictWhenChunkIndexIsAheadOfServerState() throws Exception {
        when(videoUploadSessionService.uploadChunk(eq("upload123"), eq(4), any(), eq("1")))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "upload chunk index is ahead of server state"));

        mockMvc.perform(put("/api/video-uploads/upload123/chunks/4")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("12345".getBytes())
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt.subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("upload chunk index is ahead of server state"));
    }

    @Test
    void shouldReturnNotFoundWhenUploadSessionDoesNotExist() throws Exception {
        when(videoUploadSessionService.getUploadStatus("missing-upload", "1"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "upload session not found"));

        mockMvc.perform(get("/api/video-uploads/missing-upload")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt.subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("upload session not found"));
    }

    @Test
    void shouldReturnBadRequestWhenUploadCompletionInputIsInvalid() throws Exception {
        when(videoUploadSessionService.uploadChunk(eq("upload123"), eq(0), any(), eq("1")))
                .thenThrow(new IllegalArgumentException("Upload chunk must not be empty"));

        mockMvc.perform(put("/api/video-uploads/upload123/chunks/0")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("x".getBytes())
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt.subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Upload chunk must not be empty"));
    }

    @Test
    void shouldCompleteUploadWhenUserHasVideoWriteScope() throws Exception {
        when(videoUploadSessionService.completeUpload("upload123", "1"))
                .thenReturn(new UploadVideoResponse(
                        1L,
                        "Demo Video",
                        "/api/videos/1/play",
                        LocalDateTime.of(2026, 5, 22, 12, 0)));

        mockMvc.perform(post("/api/video-uploads/upload123/complete")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt.subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Demo Video"))
                .andExpect(jsonPath("$.data.playUrl").value("/api/videos/1/play"));
    }

    @Test
    void shouldReturnForbiddenWhenUserLacksVideoWriteScope() throws Exception {
        mockMvc.perform(post("/api/video-uploads/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Demo Video",
                                  "fileName": "demo.mp4",
                                  "fileSize": 12,
                                  "contentType": "video/mp4",
                                  "chunkSize": 5,
                                  "totalChunks": 3,
                                  "fileHash": "hash123"
                                }
                                """)
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt.subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isForbidden());
    }
}
