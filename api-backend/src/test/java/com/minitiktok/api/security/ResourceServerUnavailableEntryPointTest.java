package com.minitiktok.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.client.ResourceAccessException;

class ResourceServerUnavailableEntryPointTest {

    private final ResourceServerUnavailableEntryPoint entryPoint = new ResourceServerUnavailableEntryPoint();

    @Test
    void shouldReturnServiceUnavailableWhenJwtSetCannotBeRetrieved() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationServiceException exception = new AuthenticationServiceException(
                "Couldn't retrieve remote JWK set",
                new ResourceAccessException("connect timed out"));

        entryPoint.commence(new MockHttpServletRequest(), response, exception);

        assertEquals(503, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"code\":503"));
        assertTrue(response.getContentAsString().contains("\"message\":\"authentication service unavailable\""));
    }

    @Test
    void shouldReturnServiceUnavailableForNestedConnectException() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationServiceException exception = new AuthenticationServiceException(
                "auth service down",
                new RuntimeException(new ConnectException("connection refused")));

        entryPoint.commence(new MockHttpServletRequest(), response, exception);

        assertEquals(503, response.getStatus());
    }

    @Test
    void shouldReturnServiceUnavailableForNestedSocketTimeoutException() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationServiceException exception = new AuthenticationServiceException(
                "auth service timeout",
                new RuntimeException(new SocketTimeoutException("read timed out")));

        entryPoint.commence(new MockHttpServletRequest(), response, exception);

        assertEquals(503, response.getStatus());
    }

    @Test
    void shouldDelegateToBearerEntryPointForRegularAuthenticationFailure() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new InsufficientAuthenticationException("Full authentication is required"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getHeader("WWW-Authenticate").startsWith("Bearer"));
    }
}
