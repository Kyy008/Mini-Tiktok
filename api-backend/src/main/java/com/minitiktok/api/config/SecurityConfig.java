package com.minitiktok.api.config;

import java.time.Duration;
import java.util.Collection;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.minitiktok.api.security.ResourceServerUnavailableEntryPoint;
import com.minitiktok.api.security.CookieBearerTokenResolver;

@Configuration
@Profile("!mock-auth")
@EnableConfigurationProperties(AppCorsProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            BearerTokenResolver bearerTokenResolver)
            throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/videos/recommendations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/videos/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/videos/*/play").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/videos/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/request-logs").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/request-logs").hasAuthority("SCOPE_video:read")
                        .requestMatchers(HttpMethod.GET, "/api/my/videos").hasAuthority("SCOPE_video:read")
                        .requestMatchers(HttpMethod.POST, "/api/video-uploads/init").hasAuthority("SCOPE_video:write")
                        .requestMatchers(HttpMethod.GET, "/api/video-uploads/**").hasAuthority("SCOPE_video:write")
                        .requestMatchers(HttpMethod.PUT, "/api/video-uploads/**").hasAuthority("SCOPE_video:write")
                        .requestMatchers(HttpMethod.POST, "/api/video-uploads/*/complete").hasAuthority("SCOPE_video:write")
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/views").hasAuthority("SCOPE_video:read")
                        .requestMatchers(HttpMethod.DELETE, "/api/videos/views").hasAuthority("SCOPE_video:read")
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/comments").hasAuthority("SCOPE_video:write")
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/likes").hasAuthority("SCOPE_video:like")
                        .requestMatchers(HttpMethod.DELETE, "/api/videos/*/likes").hasAuthority("SCOPE_video:like")
                        .requestMatchers(HttpMethod.GET, "/api/videos/**").hasAuthority("SCOPE_video:read")
                        .requestMatchers(HttpMethod.POST, "/api/videos").hasAuthority("SCOPE_video:write")
                        .requestMatchers(HttpMethod.DELETE, "/api/videos/*").hasAuthority("SCOPE_video:write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                            @Override
                            public <O extends BearerTokenAuthenticationFilter> O postProcess(O filter) {
                                AuthenticationEntryPointFailureHandler failureHandler = new AuthenticationEntryPointFailureHandler(
                                        authenticationEntryPoint);
                                failureHandler.setRethrowAuthenticationServiceException(false);
                                filter.setAuthenticationFailureHandler(failureHandler);
                                return filter;
                            }
                        })
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    BearerTokenResolver bearerTokenResolver() {
        return new CookieBearerTokenResolver();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        RestOperations restOperations = restTemplate;

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .restOperations(restOperations)
                .build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return jwtDecoder;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return new ResourceServerUnavailableEntryPoint();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                jwt -> (Collection<GrantedAuthority>) grantedAuthoritiesConverter.convert(jwt));
        authenticationConverter.setPrincipalClaimName("sub");
        return authenticationConverter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppCorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
