package com.minitiktok.api.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalStateException("Current authentication is not a JWT authentication");
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        String userId = jwt.getSubject();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("JWT subject is missing");
        }

        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = userId;
        }

        List<String> scopes = extractScopes(jwt.getClaim("scope"));
        return new CurrentUser(userId, username, scopes);
    }

    private List<String> extractScopes(Object scopeClaim) {
        if (scopeClaim == null) {
            return List.of();
        }

        if (scopeClaim instanceof String scopeString) {
            String normalized = scopeString.trim();
            if (normalized.isEmpty()) {
                return List.of();
            }
            return List.of(normalized.split("\\s+"));
        }

        if (scopeClaim instanceof Collection<?> scopeCollection) {
            return scopeCollection.stream()
                    .map(String::valueOf)
                    .filter(value -> !value.isBlank())
                    .toList();
        }

        return List.of();
    }
}
