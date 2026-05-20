package com.minitiktok.auth.controller;

import java.util.List;

import com.minitiktok.auth.dto.Result;
import com.minitiktok.auth.dto.ThirdPartyProfileResponse;
import com.minitiktok.auth.dto.ThirdPartyVideoResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/third-party/resources")
public class ThirdPartyResourceController {

    @GetMapping("/me")
    public Result<ThirdPartyProfileResponse> me(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return Result.success(new ThirdPartyProfileResponse(
                "mini-tiktok-mock",
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsStringList("scope")));
    }

    @GetMapping("/videos")
    public Result<List<ThirdPartyVideoResponse>> videos(JwtAuthenticationToken authentication) {
        String ownerId = authentication.getToken().getSubject();
        return Result.success(List.of(
                new ThirdPartyVideoResponse(1001L, ownerId, "Mock travel vlog", "/third-party/media/1001.mp4"),
                new ThirdPartyVideoResponse(1002L, ownerId, "Mock daily clip", "/third-party/media/1002.mp4")));
    }

    @PostMapping("/videos")
    public Result<ThirdPartyVideoResponse> createVideo(JwtAuthenticationToken authentication) {
        String ownerId = authentication.getToken().getSubject();
        return Result.success(new ThirdPartyVideoResponse(
                2001L,
                ownerId,
                "Created by mock resource server",
                "/third-party/media/2001.mp4"));
    }
}
