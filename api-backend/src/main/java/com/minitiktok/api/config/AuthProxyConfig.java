package com.minitiktok.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({AuthBackendProperties.class, OAuth2ClientProperties.class})
public class AuthProxyConfig {

    @Bean
    RestClient authBackendRestClient(RestClient.Builder builder, AuthBackendProperties properties) {
        return builder.baseUrl(properties.baseUrl()).build();
    }
}
