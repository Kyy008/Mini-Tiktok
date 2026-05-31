package com.minitiktok.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.minitiktok.api.dto.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleIllegalArgumentExceptionAsBadRequest() {
        ResponseEntity<Result<Void>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("invalid input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().code());
        assertEquals("invalid input", response.getBody().message());
    }

    @Test
    void shouldUseResponseStatusReasonWhenPresent() {
        ResponseEntity<Result<Void>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "upload is not complete yet"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().code());
        assertEquals("upload is not complete yet", response.getBody().message());
    }

    @Test
    void shouldFallbackToStatusReasonPhraseWhenResponseStatusReasonIsBlank() {
        ResponseEntity<Result<Void>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, " "));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().code());
        assertEquals("Not Found", response.getBody().message());
    }

    @Test
    void shouldHandleVideoNotFoundException() {
        ResponseEntity<Result<Void>> response = handler.handleVideoNotFound(new VideoNotFoundException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().code());
        assertEquals("video not found", response.getBody().message());
    }

    @Test
    void shouldHandleForbiddenVideoOperationException() {
        ResponseEntity<Result<Void>> response = handler.handleForbiddenVideoOperation(
                new ForbiddenVideoOperationException());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().code());
        assertEquals("forbidden", response.getBody().message());
    }
}
