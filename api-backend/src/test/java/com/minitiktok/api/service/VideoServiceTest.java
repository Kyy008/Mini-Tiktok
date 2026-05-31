package com.minitiktok.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minitiktok.api.entity.Video;
import com.minitiktok.api.mapper.VideoMapper;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Video.class);
    }

    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private VideoService videoService;

    @Test
    void shouldReturnActiveVideoWhenFindActiveByIdMatchesRecord() {
        Video video = Video.builder()
                .id(1L)
                .title("Demo Video")
                .fileHash("hash123")
                .uploaderId("uploader-1")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build();
        when(videoMapper.selectOne(any())).thenReturn(video);

        Optional<Video> result = videoService.findActiveById(1L);

        assertTrue(result.isPresent());
        assertSame(video, result.get());
    }

    @Test
    void shouldReturnEmptyWhenFindActiveByIdDoesNotMatchRecord() {
        when(videoMapper.selectOne(any())).thenReturn(null);

        Optional<Video> result = videoService.findActiveById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDelegatePageQueryForUploaderVideos() {
        Page<Video> expectedPage = new Page<Video>(2, 5, 3)
                .setRecords(java.util.List.of(
                        Video.builder()
                                .id(10L)
                                .title("My Video")
                                .fileHash("hash123")
                                .uploaderId("uploader-1")
                                .deleted(false)
                                .createdAt(LocalDateTime.of(2026, 5, 22, 10, 0))
                                .build()));
        when(videoMapper.selectPage(any(), any())).thenReturn(expectedPage);

        Page<Video> result = videoService.pageActiveByUploaderId("uploader-1", 2L, 5L);

        assertSame(expectedPage, result);

        ArgumentCaptor<Page<Video>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(videoMapper).selectPage(pageCaptor.capture(), any());
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(5L, pageCaptor.getValue().getSize());
    }

    @Test
    void shouldCreateUploadedVideoWhenInsertSucceeds() {
        when(videoMapper.insert(any(Video.class))).thenAnswer(invocation -> {
            Video video = invocation.getArgument(0);
            video.setId(1L);
            return 1;
        });

        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 20, 12, 0);
        Video result = videoService.createUploadedVideo("Demo Video", "hash123", "uploader-1", createdAt);

        assertEquals(1L, result.getId());
        assertEquals("Demo Video", result.getTitle());
        assertEquals("hash123", result.getFileHash());
        assertEquals("uploader-1", result.getUploaderId());
        assertFalse(result.getDeleted());
        assertEquals(createdAt, result.getCreatedAt());
    }

    @Test
    void shouldThrowWhenCreateUploadedVideoInsertFails() {
        when(videoMapper.insert(any(Video.class))).thenReturn(0);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> videoService.createUploadedVideo(
                        "Demo Video",
                        "hash123",
                        "uploader-1",
                        LocalDateTime.of(2026, 5, 20, 12, 0)));

        assertEquals("Failed to insert video record", exception.getMessage());
    }

    @Test
    void shouldReturnTrueWhenSoftDeleteUpdatesRow() {
        when(videoMapper.update(isNull(), any())).thenReturn(1);

        boolean result = videoService.softDeleteById(1L);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenSoftDeleteDoesNotUpdateRow() {
        when(videoMapper.update(isNull(), any())).thenReturn(0);

        boolean result = videoService.softDeleteById(1L);

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenVideoIsOwnedByUploader() {
        when(videoMapper.selectCount(any())).thenReturn(1L);

        boolean result = videoService.isOwnedBy(1L, "uploader-1");

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenVideoIsNotOwnedByUploader() {
        when(videoMapper.selectCount(any())).thenReturn(0L);

        boolean result = videoService.isOwnedBy(1L, "uploader-1");

        assertFalse(result);
    }
}
