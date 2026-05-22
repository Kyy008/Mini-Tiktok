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
@TableName("video_upload_session")
public class VideoUploadSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("upload_id")
    private String uploadId;

    @TableField("uploader_id")
    private String uploaderId;

    private String title;

    @TableField("file_name")
    private String fileName;

    @TableField("content_type")
    private String contentType;

    @TableField("file_hash")
    private String fileHash;

    @TableField("file_size")
    private Long fileSize;

    @TableField("chunk_size")
    private Integer chunkSize;

    @TableField("total_chunks")
    private Integer totalChunks;

    @TableField("next_chunk_index")
    private Integer nextChunkIndex;

    @TableField("uploaded_bytes")
    private Long uploadedBytes;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
