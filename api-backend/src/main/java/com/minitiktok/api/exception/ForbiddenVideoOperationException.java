package com.minitiktok.api.exception;

public class ForbiddenVideoOperationException extends RuntimeException {

    public ForbiddenVideoOperationException() {
        super("forbidden");
    }
}
