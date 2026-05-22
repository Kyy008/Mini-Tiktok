package com.minitiktok.api.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void shouldStoreFileWhenMp4ExtensionIsPresentEvenIfContentTypeIsGeneric() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.MP4",
                "application/octet-stream",
                "fake-mp4-content".getBytes());

        StoredVideoFile storedVideoFile = videoStorageService.store(file);

        assertEquals(file.getSize(), storedVideoFile.size());
        assertEquals("application/octet-stream", storedVideoFile.contentType());
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
    void shouldLoadStoredVideoResourceWhenFileExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.mp4",
                "video/mp4",
                "resource-content".getBytes());
        StoredVideoFile storedVideoFile = videoStorageService.store(file);

        var resource = videoStorageService.loadAsResource(storedVideoFile.fileHash());

        assertTrue(resource.isPresent());
        assertTrue(resource.get().exists());
        assertEquals(storedVideoFile.absolutePath(), resource.get().getFile().toPath());
    }

    @Test
    void shouldReturnEmptyResourceWhenStoredVideoDoesNotExist() {
        assertTrue(videoStorageService.loadAsResource("missing-hash").isEmpty());
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
    void shouldReturnZeroWhenTempUploadFileDoesNotExist() {
        assertEquals(0L, videoStorageService.getTempUploadSize("missing-upload"));
    }

    @Test
    void shouldRejectFinalizeWhenTempUploadFileDoesNotExist() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoStorageService.storeResumableUpload(
                        "missing-upload",
                        "hash123",
                        "demo.mp4",
                        "video/mp4",
                        12L));

        assertEquals("Temporary upload file does not exist", exception.getMessage());
    }

    @Test
    void shouldRejectFinalizeWhenUploadedFileSizeDoesNotMatchExpectedSize() {
        byte[] content = "chunk-content".getBytes();
        videoStorageService.appendChunk("upload-size-mismatch", content);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> videoStorageService.storeResumableUpload(
                        "upload-size-mismatch",
                        sha256(content),
                        "demo.mp4",
                        "video/mp4",
                        content.length + 1L));

        assertEquals("Uploaded file size does not match the expected size", exception.getMessage());
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

    @Test
    void shouldDeduplicateFinalizedResumableUploadAgainstExistingStoredFile() throws Exception {
        byte[] content = "same-resumable-content".getBytes();
        String expectedHash = sha256(content);
        MockMultipartFile existingFile = new MockMultipartFile("file", "existing.mp4", "video/mp4", content);
        StoredVideoFile existingStoredFile = videoStorageService.store(existingFile);
        videoStorageService.appendChunk("upload-dedup", content);

        StoredVideoFile finalizedFile = videoStorageService.storeResumableUpload(
                "upload-dedup",
                expectedHash,
                "dedup.mp4",
                "video/mp4",
                content.length);

        assertEquals(existingStoredFile.fileHash(), finalizedFile.fileHash());
        assertEquals(existingStoredFile.absolutePath(), finalizedFile.absolutePath());
        assertFalse(Files.exists(tempUploadDir.resolve("upload-dedup.part")));
        try (var files = Files.list(storageDir)) {
            assertEquals(1L, files.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void shouldDeleteTemporaryUploadFile() {
        videoStorageService.appendChunk("upload-delete", "chunk".getBytes());
        assertTrue(videoStorageService.getTempUploadSize("upload-delete") > 0L);

        videoStorageService.deleteTempUpload("upload-delete");

        assertEquals(0L, videoStorageService.getTempUploadSize("upload-delete"));
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
