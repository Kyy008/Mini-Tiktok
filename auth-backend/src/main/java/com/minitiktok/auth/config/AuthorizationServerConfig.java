package com.minitiktok.auth.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

import com.minitiktok.auth.security.AuthUserPrincipal;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            @Value("${app.oauth2.issuer:http://localhost:9000}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }

            Object principal = context.getPrincipal().getPrincipal();
            if (principal instanceof AuthUserPrincipal userPrincipal) {
                context.getClaims()
                        .subject(String.valueOf(userPrincipal.getId()))
                        .claim("preferred_username", userPrincipal.getUsername());
            }
        };
    }

    @Bean
    ApplicationRunner tiktokWebClientSeeder(
            RegisteredClientRepository registeredClientRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${app.oauth2.client.redirect-uri:http://localhost:5173/oauth/callback}") String redirectUri,
            @Value("${app.oauth2.seed-client:true}") boolean seedClient) {
        return args -> {
            if (!seedClient) {
                return;
            }
            seedTiktokWebClient(registeredClientRepository, jdbcTemplate, redirectUri);
        };
    }

    @Transactional
    void seedTiktokWebClient(
            RegisteredClientRepository registeredClientRepository,
            JdbcTemplate jdbcTemplate,
            String redirectUri) {
        RegisteredClient existingClient = registeredClientRepository.findByClientId("tiktok-web");
        if (existingClient != null && existingClient.getRedirectUris().contains(redirectUri)) {
            return;
        }
        if (existingClient != null) {
            deleteTiktokWebClient(jdbcTemplate);
        }
        registeredClientRepository.save(tiktokWebClient(redirectUri));
    }

    private void deleteTiktokWebClient(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                delete from oauth2_authorization_consent
                where registered_client_id in (
                    select id from oauth2_registered_client where client_id = ?
                )
                """, "tiktok-web");
        jdbcTemplate.update("""
                delete from oauth2_authorization
                where registered_client_id in (
                    select id from oauth2_registered_client where client_id = ?
                )
                """, "tiktok-web");
        jdbcTemplate.update("delete from oauth2_registered_client where client_id = ?", "tiktok-web");
    }

    private RegisteredClient tiktokWebClient(String redirectUri) {
        return RegisteredClient.withId("tiktok-web")
                .clientId("tiktok-web")
                .clientName("Mini-Tiktok Web")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("video:read")
                .scope("video:write")
                .scope("video:like")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(2))
                        .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();
    }

    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to generate RSA key pair", ex);
        }
    }
}
