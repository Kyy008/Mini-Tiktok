package com.minitiktok.api.security;

import java.util.List;

public record CurrentUser(String userId, String username, List<String> scopes) {
}
