package com.minitiktok.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentUserServiceTest {

    private final CurrentUserService currentUserService = new CurrentUserService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExtractCurrentUserFromJwtWithStringScopes() {
        authenticate(jwt("user-1", Map.of(
                "preferred_username", "demo",
                "scope", " video:read   video:write ")));

        CurrentUser currentUser = currentUserService.getCurrentUser();

        assertEquals("user-1", currentUser.userId());
        assertEquals("demo", currentUser.username());
        assertEquals(List.of("video:read", "video:write"), currentUser.scopes());
    }

    @Test
    void shouldFallbackUsernameToSubjectWhenPreferredUsernameIsBlank() {
        authenticate(jwt("user-1", Map.of(
                "preferred_username", " ",
                "scope", "video:read")));

        CurrentUser currentUser = currentUserService.getCurrentUser();

        assertEquals("user-1", currentUser.username());
    }

    @Test
    void shouldExtractScopesFromCollectionClaim() {
        authenticate(jwt("user-1", Map.of(
                "scope", List.of("video:read", "", "video:write"))));

        CurrentUser currentUser = currentUserService.getCurrentUser();

        assertEquals(List.of("video:read", "video:write"), currentUser.scopes());
    }

    @Test
    void shouldReturnEmptyScopesWhenScopeClaimIsUnsupportedType() {
        authenticate(jwt("user-1", Map.of("scope", 123)));

        CurrentUser currentUser = currentUserService.getCurrentUser();

        assertEquals(List.of(), currentUser.scopes());
    }

    @Test
    void shouldRejectMissingJwtAuthentication() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                currentUserService::getCurrentUser);

        assertEquals("Current authentication is not a JWT authentication", exception.getMessage());
    }

    @Test
    void shouldRejectJwtWithoutSubject() {
        authenticate(Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "video:read")
                .build());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                currentUserService::getCurrentUser);

        assertEquals("JWT subject is missing", exception.getMessage());
    }

    private void authenticate(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private Jwt jwt(String subject, Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject);
        claims.forEach(builder::claim);
        return builder.build();
    }
}
