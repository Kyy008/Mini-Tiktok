package com.minitiktok.api.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minitiktok.api.dto.Result;
import com.minitiktok.api.dto.UploadChunkResponse;
import com.minitiktok.api.dto.UploadVideoResponse;
import com.minitiktok.api.entity.RequestLog;
import com.minitiktok.api.exception.VideoNotFoundException;
import com.minitiktok.api.mapper.RequestLogMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RequestLogAspectTest {

    private RequestLogMapper requestLogMapper;
    private RequestLogAspect aspect;

    @BeforeEach
    void setUp() {
        requestLogMapper = org.mockito.Mockito.mock(RequestLogMapper.class);
        aspect = new RequestLogAspect(requestLogMapper);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                new Jwt(
                        "token",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        Map.of("alg", "none"),
                        Map.of("sub", "user-1", "preferred_username", "demo", "scope", "video:read")),
                List.of(new SimpleGrantedAuthority("SCOPE_video:read"))));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldWriteRequestLogWithResponseEntityStatus() throws Throwable {
        bindRequest("GET", "/api/videos/1");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Result.success()));

        Object result = aspect.doAround(joinPoint);

        assertTrue(result instanceof ResponseEntity);
        RequestLog log = capturedLog();
        assertEquals("user-1", log.getUserId());
        assertEquals("GET", log.getMethod());
        assertEquals("/api/videos/1", log.getPath());
        assertEquals(201, log.getStatusCode());
        assertTrue(log.getDurationMs() >= 0);
        assertEquals("[1]", log.getRequestBody());
        assertTrue(log.getResponseBody().contains("success"));
    }

    @Test
    void shouldRecordNotFoundStatusForVideoNotFoundException() throws Throwable {
        bindRequest("POST", "/api/videos/404/views");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {404L});
        when(joinPoint.proceed()).thenThrow(new VideoNotFoundException());

        assertThrows(VideoNotFoundException.class, () -> aspect.doAround(joinPoint));

        RequestLog log = capturedLog();
        assertEquals(404, log.getStatusCode());
        assertTrue(log.getResponseBody().contains("video not found"));
    }

    @Test
    void shouldSummarizeMultipartRequestWithoutFileBytes() throws Throwable {
        bindRequest("POST", "/api/videos");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.mp4",
                "video/mp4",
                "video-content".getBytes());
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {file, "Demo"});
        when(joinPoint.proceed()).thenReturn(Result.success(new UploadVideoResponse(
                1L,
                "Demo",
                "/api/videos/1/play",
                LocalDateTime.of(2026, 6, 3, 12, 0))));

        aspect.doAround(joinPoint);

        RequestLog log = capturedLog();
        assertTrue(log.getRequestBody().contains("originalFilename=demo.mp4"));
        assertTrue(log.getRequestBody().contains("contentType=video/mp4"));
        assertTrue(log.getRequestBody().contains("size=13"));
        assertFalse(log.getRequestBody().contains("video-content"));
    }

    @Test
    void shouldSummarizeChunkUploadRequestWithoutServletRequestDetails() throws Throwable {
        bindRequest("PUT", "/api/video-uploads/upload123/chunks/0");
        byte[] chunkData = "chunk-content".getBytes(StandardCharsets.UTF_8);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"upload123", 0, new MockHttpServletRequest()});
        when(joinPoint.proceed()).thenReturn(Result.success(new UploadChunkResponse(
                "upload123",
                1,
                chunkData.length,
                false)));

        aspect.doAround(joinPoint);

        RequestLog log = capturedLog();
        assertTrue(log.getRequestBody().contains("upload123"));
        assertFalse(log.getRequestBody().contains("MockHttpServletRequest"));
        assertFalse(log.getRequestBody().contains("chunk-content"));
    }

    @Test
    void shouldAvoidBinaryResponseForVideoPlayback() throws Throwable {
        bindRequest("GET", "/api/videos/1/play");
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L});
        when(joinPoint.proceed()).thenReturn(ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .body(new ByteArrayResource("video-binary".getBytes())));

        aspect.doAround(joinPoint);

        RequestLog log = capturedLog();
        assertEquals(206, log.getStatusCode());
        assertEquals("[Video Play Request: 1]", log.getRequestBody());
        assertEquals("[Binary Stream Content]", log.getResponseBody());
    }

    private void bindRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    private RequestLog capturedLog() {
        ArgumentCaptor<RequestLog> captor = ArgumentCaptor.forClass(RequestLog.class);
        verify(requestLogMapper).insert(captor.capture());
        return captor.getValue();
    }
}
