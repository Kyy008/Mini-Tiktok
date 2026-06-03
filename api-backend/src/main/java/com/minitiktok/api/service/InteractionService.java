package com.minitiktok.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minitiktok.api.entity.VideoLike;
import com.minitiktok.api.entity.VideoView;
import com.minitiktok.api.mapper.VideoLikeMapper;
import com.minitiktok.api.mapper.VideoViewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final VideoLikeMapper videoLikeMapper;
    private final VideoViewMapper videoViewMapper;

    // 点赞视频 (防重复)
    @Transactional
    public void likeVideo(String userId, Long videoId) {
        VideoLike like = VideoLike.builder()
                .userId(userId)
                .videoId(videoId)
                .createdAt(LocalDateTime.now())
                .build();
        try {
            videoLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            // 唯一索引 uk_user_video 触发，说明已点赞，保持幂等，直接返回成功
        }
    }

    // 取消点赞
    @Transactional
    public void unlikeVideo(String userId, Long videoId) {
        LambdaQueryWrapper<VideoLike> query = new LambdaQueryWrapper<VideoLike>()
                .eq(VideoLike::getUserId, userId)
                .eq(VideoLike::getVideoId, videoId);
        videoLikeMapper.delete(query);
    }

    // 获取某视频的点赞总数及当前用户的点赞状态
    public long getLikeCount(Long videoId) {
        return videoLikeMapper.selectCount(new LambdaQueryWrapper<VideoLike>().eq(VideoLike::getVideoId, videoId));
    }

    public boolean isLikedByUser(String userId, Long videoId) {
        return videoLikeMapper.selectCount(new LambdaQueryWrapper<VideoLike>()
                .eq(VideoLike::getUserId, userId)
                .eq(VideoLike::getVideoId, videoId)) > 0;
    }

    // 记录访问历史 (防重复)
    @Transactional
    public void recordView(String userId, Long videoId) {
        VideoView view = VideoView.builder()
                .userId(userId)
                .videoId(videoId)
                .viewedAt(LocalDateTime.now())
                .build();
        try {
            videoViewMapper.insert(view);
        } catch (DuplicateKeyException e) {
            // 重复看同一视频不重复插入，静默处理
        }
    }
}
