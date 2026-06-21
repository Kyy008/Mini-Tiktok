package com.minitiktok.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI miniTiktokOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini-Tiktok API")
                        .description("Mini-Tiktok 视频、互动、上传、用户和请求日志接口")
                        .version("1.0.0")
                        .contact(new Contact().name("Mini-Tiktok Team")))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入 OAuth2 获取的 access_token")));
    }
}
