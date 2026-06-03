package com.minitiktok.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("video_view")
public class VideoView {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private Long videoId;

    /**
     * 对应数据库 viewed_at datetime = CURRENT_TIMESTAMP
     */
    private LocalDateTime viewedAt;
}