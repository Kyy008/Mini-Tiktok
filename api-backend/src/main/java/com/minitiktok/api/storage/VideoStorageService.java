package com.minitiktok.api.storage;

import com.minitiktok.api.config.VideoStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
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

    public void validateMp4Metadata(String fileName, String contentType) {
        if (!isMp4(fileName, contentType)) {
            throw new IllegalArgumentException("Only MP4 video files are supported");
        }
    }

    public Optional<Resource> loadAsResource(String fileHash) {
        Path storedFilePath = resolveStoredFilePath(fileHash);
        if (!Files.exists(storedFilePath) || !Files.isRegularFile(storedFilePath)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(storedFilePath));
    }

    public void appendChunk(String uploadId, byte[] chunkData) {
        if (chunkData == null || chunkData.length == 0) {
            throw new IllegalArgumentException("Upload chunk must not be empty");
        }

        Path tempUploadPath = resolveTempUploadPath(uploadId);
        try {
            Files.createDirectories(resolveTempDir());
            Files.write(tempUploadPath, chunkData, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to append upload chunk", ex);
        }
    }

    public long getTempUploadSize(String uploadId) {
        Path tempUploadPath = resolveTempUploadPath(uploadId);
        if (!Files.exists(tempUploadPath)) {
            return 0L;
        }

        try {
            return Files.size(tempUploadPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read temporary upload size", ex);
        }
    }

    public StoredVideoFile storeResumableUpload(
            String uploadId,
            String expectedFileHash,
            String originalFileName,
            String declaredContentType,
            long expectedSize) {
        validateMp4Metadata(originalFileName, declaredContentType);

        Path tempUploadPath = resolveTempUploadPath(uploadId);
        if (!Files.exists(tempUploadPath) || !Files.isRegularFile(tempUploadPath)) {
            throw new IllegalArgumentException("Temporary upload file does not exist");
        }

        try {
            long actualSize = Files.size(tempUploadPath);
            if (actualSize != expectedSize) {
                throw new IllegalArgumentException("Uploaded file size does not match the expected size");
            }

            String actualFileHash = sha256Hex(tempUploadPath);
            if (!actualFileHash.equalsIgnoreCase(expectedFileHash)) {
                throw new IllegalArgumentException("Uploaded file hash does not match the expected hash");
            }

            Path storedFilePath = resolveStoredFilePath(actualFileHash);
            Files.createDirectories(resolveStorageDir());
            if (Files.exists(storedFilePath)) {
                Files.delete(tempUploadPath);
            } else {
                Files.move(tempUploadPath, storedFilePath);
            }

            return new StoredVideoFile(
                    actualFileHash,
                    storedFilePath.getFileName().toString(),
                    storedFilePath,
                    declaredContentType,
                    actualSize);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to finalize resumable upload", ex);
        }
    }

    public void deleteTempUpload(String uploadId) {
        Path tempUploadPath = resolveTempUploadPath(uploadId);
        try {
            Files.deleteIfExists(tempUploadPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete temporary upload file", ex);
        }
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
        return isMp4(file.getOriginalFilename(), file.getContentType());
    }

    private boolean isMp4(String fileName, String contentType) {
        if (contentType != null && MP4_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            return true;
        }
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(MP4_EXTENSION);
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

    private String sha256Hex(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path);
                    DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                digestInputStream.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded video file", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private Path resolveStorageDir() {
        return Path.of(storageProperties.getStorageDir()).toAbsolutePath().normalize();
    }

    private Path resolveTempDir() {
        return Path.of(storageProperties.getTempDir()).toAbsolutePath().normalize();
    }

    private Path resolveStoredFilePath(String fileHash) {
        return resolveStorageDir().resolve(fileHash + MP4_EXTENSION).toAbsolutePath().normalize();
    }

    private Path resolveTempUploadPath(String uploadId) {
        return resolveTempDir().resolve(uploadId + ".part").toAbsolutePath().normalize();
    }
}
