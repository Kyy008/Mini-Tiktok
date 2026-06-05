package com.minitiktok.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.minitiktok.api.dto.VideoRecommendationVO;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.mapper.VideoMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class InteractionRecommendationIntegrationTest {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from request_log");
        jdbcTemplate.update("delete from video_view");
        jdbcTemplate.update("delete from video_like");
        jdbcTemplate.update("delete from video_upload_session");
        jdbcTemplate.update("delete from video");
    }

    @Test
    void shouldKeepLikeAndViewOperationsIdempotent() {
        Video video = insertVideo("Demo", "owner", LocalDateTime.of(2026, 6, 3, 10, 0));

        interactionService.likeVideo("user-1", video.getId());
        interactionService.likeVideo("user-1", video.getId());

        assertEquals(1L, countRows("video_like", "user_id = ? and video_id = ?", "user-1", video.getId()));
        assertEquals(1L, interactionService.getLikeCount(video.getId()));
        assertEquals(true, interactionService.isLikedByUser("user-1", video.getId()));

        interactionService.unlikeVideo("user-1", video.getId());
        interactionService.unlikeVideo("user-1", video.getId());

        assertEquals(0L, countRows("video_like", "user_id = ? and video_id = ?", "user-1", video.getId()));
        assertEquals(false, interactionService.isLikedByUser("user-1", video.getId()));

        interactionService.recordView("user-1", video.getId());
        interactionService.recordView("user-1", video.getId());

        assertEquals(1L, countRows("video_view", "user_id = ? and video_id = ?", "user-1", video.getId()));
    }

    @Test
    void shouldRecommendUnviewedVideosByLikeCountThenCreatedAt() {
        Video topLiked = insertVideo("Top Liked", "owner", LocalDateTime.of(2026, 6, 3, 10, 0));
        Video newerWithOneLike = insertVideo("Newer One Like", "owner", LocalDateTime.of(2026, 6, 3, 12, 0));
        Video olderWithOneLike = insertVideo("Older One Like", "owner", LocalDateTime.of(2026, 6, 3, 11, 0));
        Video viewed = insertVideo("Viewed", "owner", LocalDateTime.of(2026, 6, 3, 13, 0));
        Video deleted = insertVideo("Deleted", "owner", LocalDateTime.of(2026, 6, 3, 14, 0), true);

        interactionService.likeVideo("user-1", topLiked.getId());
        interactionService.likeVideo("user-2", topLiked.getId());
        interactionService.likeVideo("user-2", newerWithOneLike.getId());
        interactionService.likeVideo("user-3", olderWithOneLike.getId());
        interactionService.likeVideo("user-2", viewed.getId());
        interactionService.likeVideo("user-2", deleted.getId());
        interactionService.recordView("user-1", viewed.getId());

        List<VideoRecommendationVO> recommendations = videoService.getRecommendations("user-1", 10);

        assertIterableEquals(
                List.of(topLiked.getId(), newerWithOneLike.getId(), olderWithOneLike.getId()),
                recommendations.stream().map(VideoRecommendationVO::getId).toList());
        assertEquals(2L, recommendations.get(0).getLikeCount());
        assertEquals(true, recommendations.get(0).getLiked());
        assertEquals("/api/videos/" + topLiked.getId() + "/play", recommendations.get(0).getPlayUrl());
    }

    @Test
    void shouldRecommendActiveVideosForGuestWithoutPersonalState() {
        Video first = insertVideo("First", "owner", LocalDateTime.of(2026, 6, 3, 10, 0));
        Video second = insertVideo("Second", "owner", LocalDateTime.of(2026, 6, 3, 11, 0));
        insertVideo("Deleted", "owner", LocalDateTime.of(2026, 6, 3, 12, 0), true);
        interactionService.likeVideo("user-1", first.getId());

        List<VideoRecommendationVO> recommendations = videoService.getRecommendations(null, 10);

        assertIterableEquals(
                List.of(first.getId(), second.getId()),
                recommendations.stream().map(VideoRecommendationVO::getId).toList());
        assertEquals(false, recommendations.get(0).getLiked());
    }

    @Test
    void shouldIsolateViewHistoryByUserAndReturnEmptyWhenEverythingWasViewed() {
        Video first = insertVideo("First", "owner", LocalDateTime.of(2026, 6, 3, 10, 0));
        Video second = insertVideo("Second", "owner", LocalDateTime.of(2026, 6, 3, 11, 0));

        interactionService.recordView("user-1", first.getId());
        interactionService.recordView("user-1", second.getId());

        assertEquals(List.of(), videoService.getRecommendations("user-1", 10));
        assertIterableEquals(
                List.of(second.getId(), first.getId()),
                videoService.getRecommendations("user-2", 10).stream().map(VideoRecommendationVO::getId).toList());
    }

    private Video insertVideo(String title, String uploaderId, LocalDateTime createdAt) {
        return insertVideo(title, uploaderId, createdAt, false);
    }

    private Video insertVideo(String title, String uploaderId, LocalDateTime createdAt, boolean deleted) {
        Video video = Video.builder()
                .title(title)
                .fileHash(title.toLowerCase().replace(" ", "-") + "-hash")
                .uploaderId(uploaderId)
                .deleted(deleted)
                .createdAt(createdAt)
                .build();
        videoMapper.insert(video);
        return video;
    }

    private long countRows(String table, String whereClause, Object... args) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + table + " where " + whereClause, Long.class, args);
        return count == null ? 0L : count;
    }
}
