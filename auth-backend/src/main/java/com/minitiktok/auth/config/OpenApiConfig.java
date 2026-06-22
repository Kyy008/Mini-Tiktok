package com.minitiktok.auth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI miniTiktokAuthOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini-Tiktok Auth API")
                        .description("Mini-Tiktok 登录、注册、OAuth2 授权、Token 签发和 JWK 公钥端点")
                        .version("1.0.0")
                        .contact(new Contact().name("Mini-Tiktok Team")))
                .components(new Components()
                        .addSecuritySchemes("SessionCookie", new SecurityScheme()
                                .name("JSESSIONID")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .description("登录后由鉴权服务器维护的会话 Cookie"))
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("OAuth2 access_token")))
                .paths(new Paths()
                        .addPathItem("/login", new PathItem()
                                .get(operation("登录页面", "返回鉴权服务器登录页"))
                                .post(operation("提交登录", "提交用户名、密码和 CSRF 信息完成身份认证")
                                        .requestBody(formBody("username", "password", "_csrf"))
                                        .responses(redirectResponses())))
                        .addPathItem("/register", new PathItem()
                                .get(operation("注册页面", "返回鉴权服务器注册页"))
                                .post(operation("提交注册", "创建用户账号，校验用户名唯一性并使用 BCrypt 保存密码摘要")
                                        .requestBody(formBody("username", "password"))
                                        .responses(redirectResponses())))
                        .addPathItem("/logout", new PathItem()
                                .get(operation("退出登录", "清除鉴权服务器登录会话")
                                        .responses(redirectResponses())))
                        .addPathItem("/oauth2/authorize", new PathItem()
                                .get(operation("OAuth2 授权端点", "授权码模式入口，支持 PKCE")
                                        .parameters(List.of(
                                                query("response_type", "授权响应类型，固定为 code"),
                                                query("client_id", "OAuth2 客户端 ID"),
                                                query("redirect_uri", "授权成功后的回调地址"),
                                                query("scope", "申请的权限范围"),
                                                query("state", "前端生成的防 CSRF 随机值"),
                                                query("code_challenge", "PKCE code_challenge"),
                                                query("code_challenge_method", "PKCE 摘要算法，使用 S256")))
                                        .responses(redirectResponses())))
                        .addPathItem("/oauth2/token", new PathItem()
                                .post(operation("OAuth2 Token 端点", "使用授权码和 PKCE code_verifier 换取 access_token")
                                        .requestBody(formBody(
                                                "grant_type",
                                                "client_id",
                                                "redirect_uri",
                                                "code",
                                                "code_verifier"))
                                        .responses(jsonResponses("返回 access_token、token_type、expires_in 和 scope"))))
                        .addPathItem("/oauth2/jwks", new PathItem()
                                .get(operation("JWK 公钥端点", "业务服务器通过该端点获取 JWT 签名校验公钥")
                                        .responses(jsonResponses("返回 RSA JWK Set"))))
                        .addPathItem("/.well-known/openid-configuration", new PathItem()
                                .get(operation("授权服务器元数据", "返回 OAuth2/OIDC 标准元数据信息")
                                        .responses(jsonResponses("返回 issuer、authorization_endpoint、token_endpoint 和 jwks_uri 等信息"))))
                        .addPathItem("/users/me", new PathItem()
                                .get(operation("鉴权服务器当前用户", "基于当前会话或 JWT 返回用户 ID 和用户名")
                                        .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                                        .responses(jsonResponses("返回当前用户身份信息")))));
    }

    private Operation operation(String summary, String description) {
        return new Operation()
                .summary(summary)
                .description(description)
                .tags(List.of("鉴权服务器"));
    }

    private Parameter query(String name, String description) {
        return new Parameter()
                .name(name)
                .in("query")
                .description(description)
                .schema(new StringSchema());
    }

    private RequestBody formBody(String... fieldNames) {
        ObjectSchema schema = new ObjectSchema();
        for (String fieldName : fieldNames) {
            schema.addProperty(fieldName, new StringSchema());
        }
        return new RequestBody()
                .required(true)
                .content(new Content().addMediaType(
                        "application/x-www-form-urlencoded",
                        new MediaType().schema(schema)));
    }

    private ApiResponses redirectResponses() {
        return new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("返回 HTML 页面"))
                .addApiResponse("302", new ApiResponse().description("重定向到下一步页面或前端回调地址"));
    }

    private ApiResponses jsonResponses(String description) {
        return new ApiResponses()
                .addApiResponse("200", new ApiResponse()
                        .description(description)
                        .content(new Content().addMediaType(
                                "application/json",
                                new MediaType().schema(new ObjectSchema()))));
    }
}
