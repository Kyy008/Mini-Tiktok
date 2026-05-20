package com.minitiktok.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minitiktok.api.config.MockAuthSecurityConfig;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoService;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VideoController.class)
@ActiveProfiles("mock-auth")
@Import({MockAuthSecurityConfig.class, CurrentUserService.class})
class MockAuthModeVideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoStorageService videoStorageService;

    @MockBean
    private VideoService videoService;

    @Test
    void shouldUploadVideoWhenUsingMockVideoWriteToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.mp4",
                "video/mp4",
                "video-content".getBytes());

        when(videoStorageService.store(any())).thenReturn(new StoredVideoFile(
                "hash123",
                "hash123.mp4",
                Path.of("storage/videos/hash123.mp4"),
                "video/mp4",
                file.getSize()));
        when(videoService.createUploadedVideo(eq("Demo Video"), eq("hash123"), eq("local-uploader"), any(LocalDateTime.class)))
                .thenReturn(Video.builder()
                        .id(1L)
                        .title("Demo Video")
                        .fileHash("hash123")
                        .uploaderId("local-uploader")
                        .deleted(false)
                        .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                        .build());

        mockMvc.perform(multipart("/api/videos")
                        .file(file)
                        .param("title", "Demo Video")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Demo Video"))
                .andExpect(jsonPath("$.data.playUrl").value("/api/videos/1/play"));

        verify(videoService).createUploadedVideo(eq("Demo Video"), eq("hash123"), eq("local-uploader"), any(LocalDateTime.class));
    }

    @Test
    void shouldReturnForbiddenWhenUsingMockVideoReadTokenForUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.mp4",
                "video/mp4",
                "video-content".getBytes());

        mockMvc.perform(multipart("/api/videos")
                        .file(file)
                        .param("title", "Demo Video")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-read"))
                .andExpect(status().isForbidden());
    }
}
