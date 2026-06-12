package com.minitiktok.api.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minitiktok.api.entity.VideoComment;
import com.minitiktok.api.exception.VideoNotFoundException;
import com.minitiktok.api.mapper.VideoCommentMapper;
import com.minitiktok.api.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoCommentService {

    private final VideoCommentMapper videoCommentMapper;
    private final VideoService videoService;

    public List<VideoComment> listByVideoId(Long videoId) {
        ensureVideoExists(videoId);
        LambdaQueryWrapper<VideoComment> query = new LambdaQueryWrapper<VideoComment>()
                .eq(VideoComment::getVideoId, videoId)
                .orderByDesc(VideoComment::getCreatedAt)
                .orderByDesc(VideoComment::getId);
        return videoCommentMapper.selectList(query);
    }

    public VideoComment create(Long videoId, String content, CurrentUser user) {
        ensureVideoExists(videoId);
        String normalizedContent = normalizeContent(content);
        VideoComment comment = VideoComment.builder()
                .videoId(videoId)
                .userId(user.userId())
                .usernameSnapshot(user.username())
                .content(normalizedContent)
                .createdAt(LocalDateTime.now())
                .build();
        int rows = videoCommentMapper.insert(comment);
        if (rows != 1) {
            throw new IllegalStateException("Failed to insert video comment");
        }
        return comment;
    }

    public long countByVideoId(Long videoId) {
        LambdaQueryWrapper<VideoComment> query = new LambdaQueryWrapper<VideoComment>()
                .eq(VideoComment::getVideoId, videoId);
        return videoCommentMapper.selectCount(query);
    }

    private void ensureVideoExists(Long videoId) {
        if (videoService.findActiveById(videoId).isEmpty()) {
            throw new VideoNotFoundException();
        }
    }

    private String normalizeContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        String normalized = content.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("评论内容不能超过 1000 个字符");
        }
        return normalized;
    }
}
