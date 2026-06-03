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
@TableName("request_log")
public class RequestLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String method;

    private String path;

    private String requestBody;

    private String responseBody;

    private Integer statusCode;

    private Long durationMs;

    private String ip;

    private LocalDateTime createdAt;
}