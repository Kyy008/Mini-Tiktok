package com.minitiktok.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minitiktok.api.dto.RequestLogResponse;
import com.minitiktok.api.entity.RequestLog;
import com.minitiktok.api.mapper.RequestLogMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestLogService {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 100;

    private final RequestLogMapper requestLogMapper;

    public List<RequestLogResponse> listRecentLogs(int limit, Long afterId) {
        int normalizedLimit = normalizeLimit(limit);
        LambdaQueryWrapper<RequestLog> query = new LambdaQueryWrapper<>();
        if (afterId != null && afterId > 0) {
            query.gt(RequestLog::getId, afterId);
        }
        query.orderByDesc(RequestLog::getId)
                .last("limit " + normalizedLimit);
        return requestLogMapper.selectList(query).stream()
                .map(this::toResponse)
                .toList();
    }

    public int clearLogs() {
        return requestLogMapper.deleteAll();
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Log limit must be greater than or equal to 1");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private RequestLogResponse toResponse(RequestLog log) {
        return new RequestLogResponse(
                log.getId(),
                log.getUserId(),
                log.getMethod(),
                log.getPath(),
                log.getRequestBody(),
                log.getResponseBody(),
                log.getStatusCode(),
                log.getDurationMs(),
                log.getIp(),
                log.getCreatedAt());
    }
}
