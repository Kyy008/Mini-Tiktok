package com.minitiktok.api.dto;

import java.time.LocalDateTime;

public class VideoRecommendationVO {
    private Long id;
    private String title;
    private Long likeCount;
    private Boolean liked; // 当前用户是否点赞
    private String playUrl;
    private LocalDateTime createdAt;
}