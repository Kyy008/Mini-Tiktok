package com.minitiktok.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minitiktok.api.config.SecurityConfig;
import com.minitiktok.api.dto.VideoRecommendationVO;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.InteractionService;
import com.minitiktok.api.service.VideoService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InteractionController.class)
@Import({SecurityConfig.class, CurrentUserService.class})
class InteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @MockBean
    private InteractionService interactionService;

    @Test
    void shouldReturnRecommendationsWhenUserHasVideoReadScope() throws Exception {
        when(videoService.getRecommendations("user-1", 10)).thenReturn(List.of(
                new VideoRecommendationVO(
                        1L,
                        "Recommended Video",
                        3L,
                        true,
                        "/api/videos/1/play",
                        LocalDateTime.of(2026, 6, 3, 12, 0))));

        mockMvc.perform(get("/api/videos/recommendations")
                        .with(readJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Recommended Video"))
                .andExpect(jsonPath("$.data[0].likeCount").value(3))
                .andExpect(jsonPath("$.data[0].liked").value(true))
                .andExpect(jsonPath("$.data[0].playUrl").value("/api/videos/1/play"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-06-03T12:00:00"));
    }

    @Test
    void shouldReturnGuestRecommendationsWithoutToken() throws Exception {
        when(videoService.getRecommendations(null, 10)).thenReturn(List.of(
                new VideoRecommendationVO(
                        2L,
                        "Guest Video",
                        5L,
                        false,
                        "/api/videos/2/play",
                        LocalDateTime.of(2026, 6, 3, 13, 0))));

        mockMvc.perform(get("/api/videos/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].liked").value(false));

        verify(videoService).getRecommendations(null, 10);
    }

    @Test
    void shouldRejectRecommendationSizeBelowOne() throws Exception {
        mockMvc.perform(get("/api/videos/recommendations")
                        .param("size", "0")
                        .with(readJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Recommendation size must be greater than or equal to 1"));

        verify(videoService, never()).getRecommendations(eq("user-1"), eq(0));
    }

    @Test
    void shouldCapRecommendationSizeAtFifty() throws Exception {
        when(videoService.getRecommendations("user-1", 50)).thenReturn(List.of());

        mockMvc.perform(get("/api/videos/recommendations")
                        .param("size", "100")
                        .with(readJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(videoService).getRecommendations("user-1", 50);
    }

    @Test
    void shouldRejectRecordingViewWithoutToken() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(activeVideo(1L)));

        mockMvc.perform(post("/api/videos/1/views"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRecordViewWhenUserHasVideoReadScope() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(activeVideo(1L)));

        mockMvc.perform(post("/api/videos/1/views")
                        .with(readJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(interactionService).recordView("user-1", 1L);
    }

    @Test
    void shouldReturnNotFoundWhenRecordingViewForMissingVideo() throws Exception {
        when(videoService.findActiveById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/videos/404/views")
                        .with(readJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("video not found"));

        verify(interactionService, never()).recordView(eq("user-1"), eq(404L));
    }

    @Test
    void shouldLikeVideoWhenUserHasVideoLikeScope() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(activeVideo(1L)));

        mockMvc.perform(post("/api/videos/1/likes")
                        .with(likeJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(interactionService).likeVideo("user-1", 1L);
    }

    @Test
    void shouldReturnForbiddenWhenLikingWithoutVideoLikeScope() throws Exception {
        mockMvc.perform(post("/api/videos/1/likes")
                        .with(readJwt()))
                .andExpect(status().isForbidden());

        verify(interactionService, never()).likeVideo(eq("user-1"), eq(1L));
    }

    @Test
    void shouldReturnNotFoundWhenLikingMissingVideo() throws Exception {
        when(videoService.findActiveById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/videos/404/likes")
                        .with(likeJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("video not found"));

        verify(interactionService, never()).likeVideo(eq("user-1"), eq(404L));
    }

    @Test
    void shouldUnlikeVideoWhenUserHasVideoLikeScope() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(activeVideo(1L)));

        mockMvc.perform(delete("/api/videos/1/likes")
                        .with(likeJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(interactionService).unlikeVideo("user-1", 1L);
    }

    @Test
    void shouldReturnLikeStatusWhenUserHasVideoReadScope() throws Exception {
        when(videoService.findActiveById(1L)).thenReturn(Optional.of(activeVideo(1L)));
        when(interactionService.getLikeCount(1L)).thenReturn(7L);
        when(interactionService.isLikedByUser("user-1", 1L)).thenReturn(true);

        mockMvc.perform(get("/api/videos/1/likes")
                        .with(readJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videoId").value(1))
                .andExpect(jsonPath("$.data.likeCount").value(7))
                .andExpect(jsonPath("$.data.liked").value(true));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor readJwt() {
        return jwt().authorities(() -> "SCOPE_video:read")
                .jwt(jwt -> jwt
                        .subject("user-1")
                        .claim("preferred_username", "reader")
                        .claim("scope", "video:read"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor likeJwt() {
        return jwt().authorities(() -> "SCOPE_video:like")
                .jwt(jwt -> jwt
                        .subject("user-1")
                        .claim("preferred_username", "liker")
                        .claim("scope", "video:like"));
    }

    private Video activeVideo(Long id) {
        return Video.builder()
                .id(id)
                .title("Demo")
                .fileHash("hash")
                .uploaderId("owner")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 6, 3, 12, 0))
                .build();
    }
}
