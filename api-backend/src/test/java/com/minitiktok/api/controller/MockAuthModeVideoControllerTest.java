package com.minitiktok.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minitiktok.api.config.MockAuthSecurityConfig;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.InteractionService;
import com.minitiktok.api.service.VideoCommentService;
import com.minitiktok.api.service.VideoService;
import com.minitiktok.api.storage.StoredVideoFile;
import com.minitiktok.api.storage.VideoStorageService;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

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

    @MockBean
    private InteractionService interactionService;

    @MockBean
    private VideoCommentService videoCommentService;

    @Test
    void shouldReturnVideoDetailWhenUsingMockVideoReadToken() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Demo Video")
                .fileHash("hash123")
                .uploaderId("local-uploader")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build()));
        when(interactionService.getLikeCount(1L)).thenReturn(2L);
        when(videoCommentService.countByVideoId(1L)).thenReturn(3L);
        when(interactionService.isLikedByUser("local-reader", 1L)).thenReturn(false);

        mockMvc.perform(get("/api/videos/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Demo Video"))
                .andExpect(jsonPath("$.data.playUrl").value("/api/videos/1/play"))
                .andExpect(jsonPath("$.data.uploaderId").value("local-uploader"))
                .andExpect(jsonPath("$.data.likeCount").value(2))
                .andExpect(jsonPath("$.data.commentCount").value(3))
                .andExpect(jsonPath("$.data.liked").value(false));
    }

    @Test
    void shouldStreamVideoFileWhenUsingMockVideoReadToken() throws Exception {
        byte[] videoContent = "video-binary".getBytes(StandardCharsets.UTF_8);
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Demo Video")
                .fileHash("hash123")
                .uploaderId("local-uploader")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build()));
        when(videoStorageService.loadAsResource("hash123"))
                .thenReturn(Optional.of(new ByteArrayResource(videoContent)));

        mockMvc.perform(get("/api/videos/1/play")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-read"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes(videoContent));
    }

    @Test
    void shouldStreamVideoFileWhenBearerTokenIsStoredInCookie() throws Exception {
        byte[] videoContent = "video-binary".getBytes(StandardCharsets.UTF_8);
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Demo Video")
                .fileHash("hash123")
                .uploaderId("local-uploader")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build()));
        when(videoStorageService.loadAsResource("hash123"))
                .thenReturn(Optional.of(new ByteArrayResource(videoContent)));

        mockMvc.perform(get("/api/videos/1/play")
                        .cookie(new Cookie("mini_tiktok_access_token", "mock-video-read")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes(videoContent));
    }

    @Test
    void shouldReturnPublicDetailWhenBearerCookieIsOnlyCredentialOnNonPlaybackEndpoint() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Public Video")
                .fileHash("hash123")
                .uploaderId("owner")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build()));

        mockMvc.perform(get("/api/videos/1")
                        .cookie(new Cookie("mini_tiktok_access_token", "mock-video-read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.liked").value(false));
    }

    @Test
    void shouldReturnVideoDetailWhenUsingMockVideoWriteTokenForPublicReadEndpoint() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Public Video")
                .fileHash("hash123")
                .uploaderId("local-uploader")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 22, 12, 0))
                .build()));

        mockMvc.perform(get("/api/videos/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.liked").value(false));
    }

    @Test
    void shouldReturnMyVideosPageWhenUsingMockVideoReadToken() throws Exception {
        when(videoService.pageActiveByUploaderId("local-reader", 1L, 10L))
                .thenReturn(new Page<Video>(1, 10, 1)
                        .setRecords(java.util.List.of(
                                Video.builder()
                                        .id(20L)
                                        .title("Reader Video")
                                        .fileHash("hash123")
                                        .uploaderId("local-reader")
                                        .deleted(false)
                                        .createdAt(LocalDateTime.of(2026, 5, 22, 11, 0))
                                        .build())));
        when(interactionService.getLikeCount(20L)).thenReturn(5L);

        mockMvc.perform(get("/api/my/videos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(20))
                .andExpect(jsonPath("$.data.records[0].title").value("Reader Video"))
                .andExpect(jsonPath("$.data.records[0].playUrl").value("/api/videos/20/play"))
                .andExpect(jsonPath("$.data.records[0].likeCount").value(5));
    }

    @Test
    void shouldReturnForbiddenForMyVideosWhenUsingMockVideoWriteToken() throws Exception {
        mockMvc.perform(get("/api/my/videos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-write"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteVideoWhenUsingMockVideoWriteToken() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(Video.builder()
                .id(1L)
                .title("Demo Video")
                .fileHash("hash123")
                .uploaderId("local-uploader")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build()));
        when(videoService.softDeleteById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/videos/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        verify(videoService).softDeleteById(1L);
    }

    @Test
    void shouldReturnForbiddenWhenUsingMockVideoReadTokenForDelete() throws Exception {
        mockMvc.perform(delete("/api/videos/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-video-read"))
                .andExpect(status().isForbidden());

        verify(videoService, never()).softDeleteById(any());
    }

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
