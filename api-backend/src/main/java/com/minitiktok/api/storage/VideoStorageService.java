package com.minitiktok.api.storage;

import com.minitiktok.api.config.VideoStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoStorageService {

    private static final String MP4_CONTENT_TYPE = "video/mp4";
    private static final String MP4_EXTENSION = ".mp4";

    private final VideoStorageProperties storageProperties;

    public VideoStorageService(VideoStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public StoredVideoFile store(MultipartFile file) {
        validateFile(file);

        byte[] content = readBytes(file);
        String fileHash = sha256Hex(content);
        Path storageDir = resolveStorageDir();
        Path absolutePath = resolveStoredFilePath(fileHash);

        try {
            Files.createDirectories(storageDir);
            if (!Files.exists(absolutePath)) {
                Files.write(absolutePath, content);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store video file", ex);
        }

        return new StoredVideoFile(
                fileHash,
                absolutePath.getFileName().toString(),
                absolutePath,
                file.getContentType(),
                file.getSize());
    }

    public Optional<Resource> loadAsResource(String fileHash) {
        Path storedFilePath = resolveStoredFilePath(fileHash);
        if (!Files.exists(storedFilePath) || !Files.isRegularFile(storedFilePath)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(storedFilePath));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file must not be empty");
        }
        if (!isMp4(file)) {
            throw new IllegalArgumentException("Only MP4 video files are supported");
        }
    }

    private boolean isMp4(MultipartFile file) {
        String contentType = file.getContentType();
        if (MP4_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            return true;
        }

        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(MP4_EXTENSION);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded video file", ex);
        }
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private Path resolveStorageDir() {
        return Path.of(storageProperties.getStorageDir()).toAbsolutePath().normalize();
    }

    private Path resolveStoredFilePath(String fileHash) {
        return resolveStorageDir().resolve(fileHash + MP4_EXTENSION).toAbsolutePath().normalize();
    }
}
