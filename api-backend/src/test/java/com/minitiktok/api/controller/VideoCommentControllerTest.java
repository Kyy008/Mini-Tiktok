package com.minitiktok.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.minitiktok.api.config.SecurityConfig;
import com.minitiktok.api.entity.VideoComment;
import com.minitiktok.api.exception.VideoNotFoundException;
import com.minitiktok.api.security.CurrentUser;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoCommentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VideoCommentController.class)
@Import({SecurityConfig.class, CurrentUserService.class})
class VideoCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoCommentService videoCommentService;

    @Test
    void shouldListCommentsForPublicRead() throws Exception {
        when(videoCommentService.listByVideoId(1L)).thenReturn(List.of(
                VideoComment.builder()
                        .id(2L)
                        .videoId(1L)
                        .userId("user-2")
                        .usernameSnapshot("alice")
                        .content("最新评论")
                        .createdAt(LocalDateTime.of(2026, 6, 12, 10, 1))
                        .build(),
                VideoComment.builder()
                        .id(1L)
                        .videoId(1L)
                        .userId("user-1")
                        .usernameSnapshot("bob")
                        .content("较早评论")
                        .createdAt(LocalDateTime.of(2026, 6, 12, 10, 0))
                        .build()));

        mockMvc.perform(get("/api/videos/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].username").value("alice"))
                .andExpect(jsonPath("$.data[0].content").value("最新评论"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-06-12T10:01:00"))
                .andExpect(jsonPath("$.data[1].id").value(1));
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingCommentWithoutToken() throws Exception {
        mockMvc.perform(post("/api/videos/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingCommentWithoutVideoWriteScope() throws Exception {
        mockMvc.perform(post("/api/videos/1/comments")
                        .with(jwt().authorities(() -> "SCOPE_video:read")
                                .jwt(jwt -> jwt
                                        .subject("user-1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateCommentWithCurrentUserSnapshot() throws Exception {
        when(videoCommentService.create(eq(1L), eq("hello"), any(CurrentUser.class)))
                .thenReturn(VideoComment.builder()
                        .id(3L)
                        .videoId(1L)
                        .userId("user-1")
                        .usernameSnapshot("demo")
                        .content("hello")
                        .createdAt(LocalDateTime.of(2026, 6, 12, 10, 2))
                        .build());

        mockMvc.perform(post("/api/videos/1/comments")
                        .with(jwt().authorities(() -> "SCOPE_video:write")
                                .jwt(jwt -> jwt
                                        .subject("user-1")
                                        .claim("preferred_username", "demo")
                                        .claim("scope", "video:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.videoId").value(1))
                .andExpect(jsonPath("$.data.userId").value("user-1"))
                .andExpect(jsonPath("$.data.username").value("demo"))
                .andExpect(jsonPath("$.data.content").value("hello"));

        ArgumentCaptor<CurrentUser> userCaptor = ArgumentCaptor.forClass(CurrentUser.class);
        verify(videoCommentService).create(eq(1L), eq("hello"), userCaptor.capture());
    }

    @Test
    void shouldReturnNotFoundWhenVideoDoesNotExist() throws Exception {
        when(videoCommentService.listByVideoId(99L)).thenThrow(new VideoNotFoundException());

        mockMvc.perform(get("/api/videos/99/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("video not found"));
    }
}
