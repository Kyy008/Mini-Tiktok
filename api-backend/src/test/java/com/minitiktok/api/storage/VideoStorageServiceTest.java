package com.minitiktok.api.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minitiktok.api.config.VideoStorageProperties;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class VideoStorageServiceTest {

    @TempDir
    Path tempDir;

    private VideoStorageService videoStorageService;
    private Path storageDir;
    private Path tempUploadDir;

    @BeforeEach
    void setUp() {
        storageDir = tempDir.resolve("videos");
        tempUploadDir = tempDir.resolve("uploads");
        VideoStorageProperties properties = new VideoStorageProperties();
        properties.setStorageDir(storageDir.toString());
        properties.setTempDir(tempUploadDir.toString());
        videoStorageService = new VideoStorageService(properties);
    }

    @Test
    void shouldStoreMp4FileSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.mp4",
                "video/mp4",
                "fake-mp4-content".getBytes());

        StoredVideoFile storedVideoFile = videoStorageService.store(file);

        assertEquals(storedVideoFile.fileHash() + ".mp4", storedVideoFile.storedFileName());
        assertEquals(file.getSize(), storedVideoFile.size());
        assertEquals("video/mp4", storedVideoFile.contentType());
        assertTrue(Files.exists(storedVideoFile.absolutePath()));
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.mp4",
                "video/mp4",
                new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> videoStorageService.store(file));
    }

    @Test
    void shouldRejectNonMp4File() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.txt",
                "text/plain",
                "plain-text".getBytes());

        assertThrows(IllegalArgumentException.class, () -> videoStorageService.store(file));
    }

    @Test
    void shouldDeduplicateFilesWithSameContent() throws IOException {
        byte[] content = "same-content".getBytes();
        MockMultipartFile firstFile = new MockMultipartFile("file", "first.mp4", "video/mp4", content);
        MockMultipartFile secondFile = new MockMultipartFile("file", "second.mp4", "video/mp4", content);

        StoredVideoFile firstStoredFile = videoStorageService.store(firstFile);
        StoredVideoFile secondStoredFile = videoStorageService.store(secondFile);

        assertEquals(firstStoredFile.fileHash(), secondStoredFile.fileHash());
        assertEquals(firstStoredFile.storedFileName(), secondStoredFile.storedFileName());
        try (var files = Files.list(storageDir)) {
            assertEquals(1L, files.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void shouldAppendChunksAndFinalizeResumableUpload() throws Exception {
        byte[] firstChunk = "first-".getBytes();
        byte[] secondChunk = "second".getBytes();
        byte[] fullContent = concat(firstChunk, secondChunk);
        String expectedHash = sha256(fullContent);

        videoStorageService.appendChunk("upload-1", firstChunk);
        videoStorageService.appendChunk("upload-1", secondChunk);

        assertEquals(fullContent.length, videoStorageService.getTempUploadSize("upload-1"));

        StoredVideoFile storedVideoFile = videoStorageService.storeResumableUpload(
                "upload-1",
                expectedHash,
                "demo.mp4",
                "video/mp4",
                fullContent.length);

        assertEquals(expectedHash, storedVideoFile.fileHash());
        assertTrue(Files.exists(storedVideoFile.absolutePath()));
        assertTrue(Files.notExists(tempUploadDir.resolve("upload-1.part")));
    }

    @Test
    void shouldRejectFinalizeWhenHashDoesNotMatch() {
        byte[] content = "chunk-content".getBytes();

        videoStorageService.appendChunk("upload-2", content);

        assertThrows(IllegalArgumentException.class, () -> videoStorageService.storeResumableUpload(
                "upload-2",
                "deadbeef",
                "demo.mp4",
                "video/mp4",
                content.length));
    }

    private byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private String sha256(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(content));
    }
}
