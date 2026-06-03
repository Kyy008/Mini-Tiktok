package com.minitiktok.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoLikeStatusResponse {
    private Long videoId;
    private Long likeCount;
    private Boolean liked;
}