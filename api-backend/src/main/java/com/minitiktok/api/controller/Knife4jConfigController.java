package com.minitiktok.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
public class Knife4jConfigController {

    @GetMapping("/v3/api-docs/swagger-config")
    public Map<String, Object> swaggerConfig() {
        return Map.of(
                "configUrl", "/v3/api-docs/swagger-config",
                "urls", List.of(Map.of(
                        "name", "mini-tiktok-api",
                        "url", "/v3/api-docs/mini-tiktok-api")),
                "validatorUrl", "");
    }
}
