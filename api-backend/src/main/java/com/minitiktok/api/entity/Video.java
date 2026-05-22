package com.minitiktok.api.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("video")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    @TableField("file_hash")
    private String fileHash;

    @TableField("uploader_id")
    private String uploaderId;

    private Boolean deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
