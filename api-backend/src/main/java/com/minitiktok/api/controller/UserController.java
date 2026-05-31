package com.minitiktok.api.controller;

import com.minitiktok.api.dto.CurrentUserResponse;
import com.minitiktok.api.dto.Result;
import com.minitiktok.api.security.CurrentUser;
import com.minitiktok.api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final CurrentUserService currentUserService;

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
