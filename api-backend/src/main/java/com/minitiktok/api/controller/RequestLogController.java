package com.minitiktok.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minitiktok.api.dto.RequestLogResponse;
import com.minitiktok.api.dto.Result;
import com.minitiktok.api.service.RequestLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "请求日志", description = "查询和清理接口调用日志")
public class RequestLogController {

    private final RequestLogService requestLogService;

    @Operation(summary = "查询请求日志")
    @GetMapping("/api/request-logs")
    public Result<List<RequestLogResponse>> listRequestLogs(
            @RequestParam(name = "limit", defaultValue = "" + RequestLogService.DEFAULT_LIMIT) int limit,
            @RequestParam(name = "afterId", defaultValue = "0") Long afterId) {
        return Result.success(requestLogService.listRecentLogs(limit, afterId));
    }

    @Operation(summary = "清空请求日志")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/api/request-logs")
    public Result<Integer> clearRequestLogs() {
        return Result.success(requestLogService.clearLogs());
    }
}
