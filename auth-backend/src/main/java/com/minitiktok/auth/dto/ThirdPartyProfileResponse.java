package com.minitiktok.auth.dto;

import java.util.List;

public record ThirdPartyProfileResponse(
        String provider,
        String userId,
        String username,
        List<String> scopes) {
}
