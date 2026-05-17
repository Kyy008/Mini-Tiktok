package com.minitiktok.auth.controller;

import com.minitiktok.auth.dto.Result;
import com.minitiktok.auth.dto.UserProfileResponse;
import com.minitiktok.auth.security.AuthUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/users/me")
    public Result<UserProfileResponse> me(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthUserPrincipal userPrincipal) {
            return Result.success(new UserProfileResponse(userPrincipal.getId(), userPrincipal.getUsername()));
        }
        if (principal instanceof Jwt jwt) {
            return Result.success(new UserProfileResponse(parseLong(jwt.getSubject()),
                    jwt.getClaimAsString("preferred_username")));
        }
        return Result.success(new UserProfileResponse(null, authentication.getName()));
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
