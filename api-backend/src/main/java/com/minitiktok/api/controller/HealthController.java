package com.minitiktok.api.controller;

import java.util.Map;

import com.minitiktok.api.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "系统状态", description = "服务健康检查")
public class HealthController {

    @Operation(summary = "检查业务服务状态")
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of("status", "ok"));
    }
}
