package com.minitiktok.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minitiktok.api.config.SecurityConfig;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoService;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
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
    void shouldReturnVideoDetailWhenUserHasVideoReadScope() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Demo Video")
                .fileHash("hash123")
                .uploaderId("uploader-1")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build()));

        mockMvc.perform(get("/api/videos/1")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Demo Video"))
                .andExpect(jsonPath("$.data.playUrl").value("/api/videos/1/play"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-20T12:00:00"))
                .andExpect(jsonPath("$.data.uploaderId").value("uploader-1"));
    }

    @Test
    void shouldReturnNotFoundWhenVideoDetailDoesNotExist() throws Exception {
        when(videoService.findActiveById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/videos/99")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("video not found"));
    }

    @Test
    void shouldStreamVideoFileWhenUserHasVideoReadScope() throws Exception {
        byte[] videoContent = "video-binary".getBytes(StandardCharsets.UTF_8);
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Demo Video")
                .fileHash("hash123")
                .uploaderId("uploader-1")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build()));
        when(videoStorageService.loadAsResource("hash123"))
                .thenReturn(Optional.of(new ByteArrayResource(videoContent)));

        mockMvc.perform(get("/api/videos/1/play")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes(videoContent));
    }

    @Test
    void shouldReturnNotFoundWhenStoredVideoFileIsMissing() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Demo Video")
                .fileHash("missing-hash")
                .uploaderId("uploader-1")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build()));
        when(videoStorageService.loadAsResource("missing-hash")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/videos/1/play")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenForReadEndpointsWithoutVideoReadScope() throws Exception {
        mockMvc.perform(get("/api/videos/1")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnMyVideosPageForAuthenticatedUser() throws Exception {
        when(videoService.pageActiveByUploaderId("1", 2L, 1L))
                .thenReturn(new Page<Video>(2, 1, 3)
                        .setRecords(java.util.List.of(
                                Video.builder()
                                        .id(10L)
                                        .title("My Video")
                                        .fileHash("hash123")
                                        .uploaderId("1")
                                        .deleted(false)
                                        .createdAt(LocalDateTime.of(2026, 5, 22, 10, 0))
                                        .build())));

        mockMvc.perform(get("/api/my/videos")
                        .param("page", "2")
                        .param("size", "1")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(10))
                .andExpect(jsonPath("$.data.records[0].title").value("My Video"))
                .andExpect(jsonPath("$.data.records[0].playUrl").value("/api/videos/10/play"))
                .andExpect(jsonPath("$.data.records[0].createdAt").value("2026-05-22T10:00:00"));
    }

    @Test
    void shouldReturnBadRequestWhenMyVideosPageParameterIsInvalid() throws Exception {
        mockMvc.perform(get("/api/my/videos")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Page number must be greater than or equal to 1"));
    }

    @Test
    void shouldReturnUnauthorizedForMyVideosWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/my/videos")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDeleteVideoWhenUserOwnsItAndHasVideoWriteScope() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Owned Video")
                .fileHash("hash123")
                .uploaderId("1")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 22, 12, 0))
                .build()));
        when(videoService.softDeleteById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/videos/1")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        verify(videoService).softDeleteById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingVideo() throws Exception {
        when(videoService.findActiveById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/videos/99")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("video not found"));

        verify(videoService, never()).softDeleteById(any());
    }

    @Test
    void shouldReturnForbiddenWhenDeletingOthersVideo() throws Exception {
        when(videoService.findActiveById(2L)).thenReturn(Optional.of(Video.builder()
                .id(2L)
                .title("Others Video")
                .fileHash("hash999")
                .uploaderId("other-user")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 22, 12, 0))
                .build()));

        mockMvc.perform(delete("/api/videos/2")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("forbidden"));

        verify(videoService, never()).softDeleteById(any());
    }

    @Test
    void shouldReturnForbiddenWhenDeletingWithoutVideoWriteScope() throws Exception {
        mockMvc.perform(delete("/api/videos/1")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedWhenDeletingWithoutAuthentication() throws Exception {
        mockMvc.perform(delete("/api/videos/1"))
                .andExpect(status().isUnauthorized());
    }

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
