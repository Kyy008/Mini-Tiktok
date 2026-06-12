package com.minitiktok.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import com.minitiktok.api.entity.Video;
import com.minitiktok.api.entity.VideoComment;
import com.minitiktok.api.exception.VideoNotFoundException;
import com.minitiktok.api.mapper.VideoMapper;
import com.minitiktok.api.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class VideoCommentServiceTest {

    @Autowired
    private VideoCommentService videoCommentService;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from request_log");
        jdbcTemplate.update("delete from video_comment");
        jdbcTemplate.update("delete from video_view");
        jdbcTemplate.update("delete from video_like");
        jdbcTemplate.update("delete from video_upload_session");
        jdbcTemplate.update("delete from video");
    }

    @Test
    void shouldReturnEmptyListWhenVideoHasNoComments() {
        Video video = insertVideo("No Comment Video", false);

        assertEquals(List.of(), videoCommentService.listByVideoId(video.getId()));
        assertEquals(0L, videoCommentService.countByVideoId(video.getId()));
    }

    @Test
    void shouldAllowSameUserToCreateMultipleCommentsAndListNewestFirst() {
        Video video = insertVideo("Comment Video", false);
        CurrentUser user = new CurrentUser("user-1", "alice", List.of("video:write"));

        VideoComment first = videoCommentService.create(video.getId(), "第一条", user);
        VideoComment second = videoCommentService.create(video.getId(), "第二条", user);

        List<VideoComment> comments = videoCommentService.listByVideoId(video.getId());

        assertEquals(2, comments.size());
        assertEquals(second.getId(), comments.get(0).getId());
        assertEquals("第二条", comments.get(0).getContent());
        assertEquals(first.getId(), comments.get(1).getId());
        assertEquals(2L, videoCommentService.countByVideoId(video.getId()));
    }

    @Test
    void shouldTrimContentAndSnapshotUsername() {
        Video video = insertVideo("Snapshot Video", false);
        CurrentUser user = new CurrentUser("user-1", "alice", List.of("video:write"));

        VideoComment comment = videoCommentService.create(video.getId(), "  hello  ", user);

        assertEquals("user-1", comment.getUserId());
        assertEquals("alice", comment.getUsernameSnapshot());
        assertEquals("hello", comment.getContent());
    }

    @Test
    void shouldRejectMissingOrDeletedVideo() {
        Video deletedVideo = insertVideo("Deleted Video", true);
        CurrentUser user = new CurrentUser("user-1", "alice", List.of("video:write"));

        assertThrows(VideoNotFoundException.class, () -> videoCommentService.listByVideoId(deletedVideo.getId()));
        assertThrows(VideoNotFoundException.class, () -> videoCommentService.create(999L, "hello", user));
    }

    @Test
    void shouldRejectBlankCommentContent() {
        Video video = insertVideo("Blank Comment Video", false);
        CurrentUser user = new CurrentUser("user-1", "alice", List.of("video:write"));

        assertThrows(IllegalArgumentException.class, () -> videoCommentService.create(video.getId(), "   ", user));
    }

    private Video insertVideo(String title, boolean deleted) {
        Video video = Video.builder()
                .title(title)
                .fileHash(title.toLowerCase().replace(" ", "-") + "-hash")
                .uploaderId("owner")
                .deleted(deleted)
                .createdAt(LocalDateTime.of(2026, 6, 12, 10, 0))
                .build();
        videoMapper.insert(video);
        return video;
    }
}
