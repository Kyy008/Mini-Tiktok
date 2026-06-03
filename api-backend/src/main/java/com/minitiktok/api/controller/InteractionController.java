package com.minitiktok.api.controller;

import com.minitiktok.api.dto.Result;
import com.minitiktok.api.dto.VideoLikeStatusResponse;
import com.minitiktok.api.dto.VideoRecommendationVO;
import com.minitiktok.api.exception.VideoNotFoundException;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.InteractionService;
import com.minitiktok.api.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class InteractionController {

    private final CurrentUserService currentUserService;
    private final VideoService videoService;
    private final InteractionService interactionService;

    // 1. 推荐接口：GET /api/videos/recommendations?size=10
    @GetMapping("/api/videos/recommendations")
    public Result<List<VideoRecommendationVO>> getRecommendations(
            @RequestParam(name = "size", defaultValue = "10") int size) {
        if (size > 50) size = 50; // 对应方案要求的上限校验
        String userId = currentUserService.getCurrentUser().userId();
        List<VideoRecommendationVO> recommendations = videoService.getRecommendations(userId, size);
        return Result.success(recommendations);
    }

    // 2. 点赞视频：POST /api/videos/{id}/likes
    @PostMapping("/api/videos/{id}/likes")
    public Result<Void> likeVideo(@PathVariable("id") Long id) {
        // 核心要求：删除或不存在的视频返回 404
        videoService.findActiveById(id).orElseThrow(VideoNotFoundException::new);

        String userId = currentUserService.getCurrentUser().userId();
        interactionService.likeVideo(userId, id);
        return Result.success();
    }

    // 3. 取消点赞：DELETE /api/videos/{id}/likes
    @DeleteMapping("/api/videos/{id}/likes")
    public Result<Void> unlikeVideo(@PathVariable("id") Long id) {
        // 未点赞或不存在的保持稳定返回成功，但仍需验证视频活跃性
        videoService.findActiveById(id).orElseThrow(VideoNotFoundException::new);

        String userId = currentUserService.getCurrentUser().userId();
        interactionService.unlikeVideo(userId, id);
        return Result.success();
    }

    // 4. 查询点赞状态与数量：GET /api/videos/{id}/likes
    @GetMapping("/api/videos/{id}/likes")
    public Result<VideoLikeStatusResponse> getLikeStatus(@PathVariable("id") Long id) {
        videoService.findActiveById(id).orElseThrow(VideoNotFoundException::new);

        String userId = currentUserService.getCurrentUser().userId();
        long likeCount = interactionService.getLikeCount(id);
        boolean liked = interactionService.isLikedByUser(userId, id);

        return Result.success(new VideoLikeStatusResponse(id, likeCount, liked));
    }

    // 5. 记录访问记录接口：POST /api/videos/{id}/views
    @PostMapping("/api/videos/{id}/views")
    public Result<Void> recordVideoView(@PathVariable("id") Long id) {
        videoService.findActiveById(id).orElseThrow(VideoNotFoundException::new);

        String userId = currentUserService.getCurrentUser().userId();
        interactionService.recordView(userId, id);
        return Result.success();
    }
}