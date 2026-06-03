package com.minitiktok.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecommendationVO {
    private Long id;
    private String title;
    private Long likeCount;
    private Boolean liked;
    private String playUrl;
    private LocalDateTime createdAt;
}
