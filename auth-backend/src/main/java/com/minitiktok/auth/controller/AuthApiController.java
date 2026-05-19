package com.minitiktok.auth.controller;

import com.minitiktok.auth.dto.RegisterRequest;
import com.minitiktok.auth.dto.Result;
import com.minitiktok.auth.dto.UserProfileResponse;
import com.minitiktok.auth.entity.User;
import com.minitiktok.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthApiController {

    private final UserService userService;

    /**
     * 提供给 Vue SPA 直接调用的 JSON 注册接口，注册成功后返回用户基础资料。
     */
    @PostMapping("/register")
    public ResponseEntity<Result<UserProfileResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(Result.success(new UserProfileResponse(user.getId(), user.getUsername())));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Result.failure(HttpStatus.CONFLICT.value(), ex.getMessage()));
        }
    }
}
