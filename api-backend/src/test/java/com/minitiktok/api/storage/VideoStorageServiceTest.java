package com.minitiktok.api.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minitiktok.api.config.VideoStorageProperties;
import java.io.IOException;
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

    @BeforeEach
    void setUp() {
        storageDir = tempDir.resolve("videos");
        VideoStorageProperties properties = new VideoStorageProperties();
        properties.setStorageDir(storageDir.toString());
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
}
