package com.minitiktok.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.minitiktok.api.dto.VideoRecommendationVO;
import com.minitiktok.api.entity.VideoView;
import com.minitiktok.api.mapper.VideoViewMapper;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoMapper videoMapper;
    private final VideoViewMapper videoViewMapper;

    public Optional<Video> findActiveById(Long id) {
        LambdaQueryWrapper<Video> query = new LambdaQueryWrapper<Video>()
                .eq(Video::getId, id)
                .eq(Video::getDeleted, false);
        return Optional.ofNullable(videoMapper.selectOne(query));
    }

    public Page<Video> pageActiveByUploaderId(String uploaderId, long page, long size) {
        Page<Video> pageRequest = Page.of(page, size);
        LambdaQueryWrapper<Video> query = new LambdaQueryWrapper<Video>()
                .eq(Video::getUploaderId, uploaderId)
                .eq(Video::getDeleted, false)
                .orderByDesc(Video::getCreatedAt);
        return videoMapper.selectPage(pageRequest, query);
    }

    public Video save(Video video) {
        int rows = videoMapper.insert(video);
        if (rows != 1) {
            throw new IllegalStateException("Failed to insert video record");
        }
        return video;
    }

    public Video createUploadedVideo(String title, String fileHash, String uploaderId, LocalDateTime createdAt) {
        Video video = Video.builder()
                .title(title)
                .fileHash(fileHash)
                .uploaderId(uploaderId)
                .deleted(false)
                .createdAt(createdAt)
                .build();
        return save(video);
    }

    public boolean softDeleteById(Long id) {
        LambdaUpdateWrapper<Video> update = new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, id)
                .eq(Video::getDeleted, false)
                .set(Video::getDeleted, true);
        return videoMapper.update(null, update) > 0;
    }

    public boolean isOwnedBy(Long videoId, String uploaderId) {
        LambdaQueryWrapper<Video> query = new LambdaQueryWrapper<Video>()
                .eq(Video::getId, videoId)
                .eq(Video::getUploaderId, uploaderId)
                .eq(Video::getDeleted, false);
        return videoMapper.selectCount(query) > 0;
    }

    public List<VideoRecommendationVO> getRecommendations(String userId, int size) {
        List<Long> viewedIds = videoViewMapper.selectList(
                new LambdaQueryWrapper<VideoView>().eq(VideoView::getUserId, userId)
        ).stream().map(VideoView::getVideoId).toList();

        // 2. 查询推荐视频 (此处推荐使用 Mapper XML 或 @Select 注解)
        // 逻辑：过滤 deleted=0，过滤 viewedIds，按点赞数降序
        return videoMapper.findRecommendations(userId, viewedIds, size);
    }
}
