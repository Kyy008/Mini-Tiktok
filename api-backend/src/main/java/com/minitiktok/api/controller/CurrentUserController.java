package com.minitiktok.api.controller;

import java.util.Collection;
import java.util.List;

import com.minitiktok.api.dto.CurrentUserResponse;
import com.minitiktok.api.dto.Result;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CurrentUserController {

    /**
     * 从资源服务器校验后的 JWT 中提取当前登录用户信息。
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return Result.success(new CurrentUserResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                scopes(jwt.getClaims().get("scope"))));
    }

    /**
     * 兼容授权服务器返回的字符串或数组两种 scope 表达形式。
     */
    private List<String> scopes(Object claim) {
        if (claim instanceof String scopeText) {
            return List.of(scopeText.split(" "));
        }
        if (claim instanceof Collection<?> scopeCollection) {
            return scopeCollection.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
