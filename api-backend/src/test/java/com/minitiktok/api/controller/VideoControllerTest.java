package com.minitiktok.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minitiktok.api.config.SecurityConfig;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoService;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VideoController.class)
@Import({SecurityConfig.class, CurrentUserService.class})
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoStorageService videoStorageService;

    @MockBean
    private VideoService videoService;

    @Test
    void shouldUploadVideoWhenUserHasVideoWriteScope() throws Exception {
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
        when(videoService.createUploadedVideo(eq("Demo Video"), eq("hash123"), eq("1"), any(LocalDateTime.class)))
                .thenReturn(Video.builder()
                        .id(1L)
                        .title("Demo Video")
                        .fileHash("hash123")
                        .uploaderId("1")
                        .deleted(false)
                        .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                        .build());

        mockMvc.perform(multipart("/api/videos")
                        .file(file)
                        .param("title", "  Demo Video  ")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Demo Video"))
                .andExpect(jsonPath("$.data.playUrl").value("/api/videos/1/play"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-20T12:00:00"));

        verify(videoStorageService).store(any());
        verify(videoService).createUploadedVideo(eq("Demo Video"), eq("hash123"), eq("1"), any(LocalDateTime.class));
    }

    @Test
    void shouldReturnForbiddenWhenUserLacksVideoWriteScope() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.mp4",
                "video/mp4",
                "video-content".getBytes());

        mockMvc.perform(multipart("/api/videos")
                        .file(file)
                        .param("title", "Demo Video")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateTwoVideoRecordsForSameStoredFileAcrossTwoUploads() throws Exception {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file",
                "first.mp4",
                "video/mp4",
                "same-content".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile(
                "file",
                "second.mp4",
                "video/mp4",
                "same-content".getBytes());

        when(videoStorageService.store(any())).thenReturn(new StoredVideoFile(
                "samehash",
                "samehash.mp4",
                Path.of("storage/videos/samehash.mp4"),
                "video/mp4",
                firstFile.getSize()));
        when(videoService.createUploadedVideo(eq("Same Video"), eq("samehash"), eq("1"), any(LocalDateTime.class)))
                .thenReturn(Video.builder()
                                .id(10L)
                                .title("Same Video")
                                .fileHash("samehash")
                                .uploaderId("1")
                                .deleted(false)
                                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                                .build())
                .thenReturn(Video.builder()
                        .id(11L)
                        .title("Same Video")
                        .fileHash("samehash")
                        .uploaderId("1")
                        .deleted(false)
                        .createdAt(LocalDateTime.of(2026, 5, 20, 12, 1))
                        .build());

        mockMvc.perform(multipart("/api/videos")
                        .file(firstFile)
                        .param("title", "Same Video")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));

        mockMvc.perform(multipart("/api/videos")
                        .file(secondFile)
                        .param("title", "Same Video")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11));

        ArgumentCaptor<String> uploaderCaptor = ArgumentCaptor.forClass(String.class);
        verify(videoService, times(2))
                .createUploadedVideo(eq("Same Video"), eq("samehash"), uploaderCaptor.capture(), any(LocalDateTime.class));
        verify(videoStorageService, times(2)).store(any());
        org.junit.jupiter.api.Assertions.assertEquals("1", uploaderCaptor.getAllValues().get(0));
        org.junit.jupiter.api.Assertions.assertEquals("1", uploaderCaptor.getAllValues().get(1));
    }
}
