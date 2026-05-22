package com.minitiktok.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.minitiktok.api.config.MockAuthSecurityConfig;
import com.minitiktok.api.dto.UploadChunkResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.dto.VideoUploadSessionResponse;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoUploadSessionService;

@WebMvcTest(VideoUploadController.class)
@ActiveProfiles("mock-auth")
@Import({MockAuthSecurityConfig.class, CurrentUserService.class})
class MockAuthModeVideoUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoUploadSessionService videoUploadSessionService;

    @Test
    void shouldInitUploadSessionWhenUsingMockVideoWriteToken() throws Exception {
        when(videoUploadSessionService.initUpload(any(), eq("local-uploader")))
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
                        .header("Authorization", "Bearer mock-video-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadId").value("upload123"))
                .andExpect(jsonPath("$.data.status").value("UPLOADING"));
    }

    @Test
    void shouldUploadChunkWhenUsingMockVideoWriteToken() throws Exception {
        when(videoUploadSessionService.uploadChunk(eq("upload123"), eq(0), any(), eq("local-uploader")))
                .thenReturn(new UploadChunkResponse("upload123", 1, 5L, false));

        mockMvc.perform(put("/api/video-uploads/upload123/chunks/0")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("12345".getBytes())
                        .header("Authorization", "Bearer mock-video-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextChunkIndex").value(1))
                .andExpect(jsonPath("$.data.uploadedBytes").value(5));
    }

    @Test
    void shouldCompleteUploadWhenUsingMockVideoWriteToken() throws Exception {
        when(videoUploadSessionService.completeUpload("upload123", "local-uploader"))
                .thenReturn(new UploadVideoResponse(
                        1L,
                        "Demo Video",
                        "/api/videos/1/play",
                        LocalDateTime.of(2026, 5, 22, 12, 0)));

        mockMvc.perform(post("/api/video-uploads/upload123/complete")
                        .header("Authorization", "Bearer mock-video-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Demo Video"));
    }

    @Test
    void shouldReturnForbiddenWhenUsingMockVideoReadTokenForUploadSessionApis() throws Exception {
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
                        .header("Authorization", "Bearer mock-video-read"))
                .andExpect(status().isForbidden());
    }
}
