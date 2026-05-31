package com.minitiktok.api.exception;

public class VideoNotFoundException extends RuntimeException {

    public VideoNotFoundException() {
        super("video not found");
    }
}
