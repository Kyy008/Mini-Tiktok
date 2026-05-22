package com.minitiktok.api.storage;

import java.nio.file.Path;

public record StoredVideoFile(
        String fileHash,
        String storedFileName,
        Path absolutePath,
        String contentType,
        long size) {
}
