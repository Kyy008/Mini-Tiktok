package com.minitiktok.api.security;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minitiktok.api.dto.Result;

public class ResourceServerUnavailableEntryPoint implements AuthenticationEntryPoint {

    private static final String MESSAGE = "authentication service unavailable";

    private final BearerTokenAuthenticationEntryPoint fallbackEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        if (!isAuthenticationServiceUnavailable(authException)) {
            fallbackEntryPoint.commence(request, response, authException);
            return;
        }

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                Result.failure(HttpStatus.SERVICE_UNAVAILABLE.value(), MESSAGE));
    }

    private boolean isAuthenticationServiceUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceAccessException
                    || current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof UnknownHostException) {
                return true;
            }

            String className = current.getClass().getSimpleName();
            if ("RemoteKeySourceException".equals(className) || "RemoteResourceException".equals(className)) {
                return true;
            }

            String message = current.getMessage();
            if (message != null && message.contains("Couldn't retrieve remote JWK set")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
