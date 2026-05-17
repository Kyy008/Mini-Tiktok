package com.minitiktok.api.dto;

import java.util.List;

public record CurrentUserResponse(String userId, String username, List<String> scopes) {
}
