package com.minitiktok.api.controller;

import com.minitiktok.api.dto.CurrentUserResponse;
import com.minitiktok.api.dto.Result;
import com.minitiktok.api.security.CurrentUser;
import com.minitiktok.api.security.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "用户", description = "当前登录用户信息")
public class UserController {

    private final CurrentUserService currentUserService;

    @Operation(summary = "获取当前用户")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/api/me")
    public Result<CurrentUserResponse> getCurrentUser() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        CurrentUserResponse response = new CurrentUserResponse(
                currentUser.userId(),
                currentUser.username(),
                currentUser.scopes());
        return Result.success(response);
    }
}
