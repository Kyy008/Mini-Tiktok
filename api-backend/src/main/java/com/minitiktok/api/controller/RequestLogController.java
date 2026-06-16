package com.minitiktok.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minitiktok.api.dto.RequestLogResponse;
import com.minitiktok.api.dto.Result;
import com.minitiktok.api.service.RequestLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RequestLogController {

    private final RequestLogService requestLogService;

    @GetMapping("/api/request-logs")
    public Result<List<RequestLogResponse>> listRequestLogs(
            @RequestParam(name = "limit", defaultValue = "" + RequestLogService.DEFAULT_LIMIT) int limit,
            @RequestParam(name = "afterId", defaultValue = "0") Long afterId) {
        return Result.success(requestLogService.listRecentLogs(limit, afterId));
    }

    @DeleteMapping("/api/request-logs")
    public Result<Integer> clearRequestLogs() {
        return Result.success(requestLogService.clearLogs());
    }
}
