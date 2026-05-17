package com.minitiktok.api.controller;

import com.minitiktok.api.dto.AuthLoginUrlResponse;
import com.minitiktok.api.dto.AuthRegisterResult;
import com.minitiktok.api.dto.AuthUserProfileResponse;
import com.minitiktok.api.dto.RegisterRequest;
import com.minitiktok.api.dto.Result;
import com.minitiktok.api.service.AuthProxyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthProxyController {

    private final AuthProxyService authProxyService;

    /**
     * 生成 OAuth2 授权登录地址，前端拿到后跳转到 auth-backend 登录页。
     */
    @GetMapping("/login-url")
    public Result<AuthLoginUrlResponse> loginUrl() {
        return Result.success(authProxyService.createLoginUrl());
    }

    /**
     * 代理前端注册请求，保持前端只访问 api-backend 的统一入口。
     */
    @PostMapping("/register")
    public ResponseEntity<Result<AuthUserProfileResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthRegisterResult result = authProxyService.register(request);
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
