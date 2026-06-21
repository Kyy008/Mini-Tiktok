package com.minitiktok.api.controller;

import java.util.List;

import com.minitiktok.api.dto.CreateCommentRequest;
import com.minitiktok.api.dto.Result;
import com.minitiktok.api.dto.VideoCommentResponse;
import com.minitiktok.api.entity.VideoComment;
import com.minitiktok.api.security.CurrentUser;
import com.minitiktok.api.security.CurrentUserService;
import com.minitiktok.api.service.VideoCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "视频评论", description = "视频评论发布与查询")
public class VideoCommentController {

    private final VideoCommentService videoCommentService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "查询视频评论")
    @GetMapping("/api/videos/{id}/comments")
    public Result<List<VideoCommentResponse>> listComments(@PathVariable("id") Long videoId) {
        List<VideoCommentResponse> comments = videoCommentService.listByVideoId(videoId).stream()
                .map(this::toResponse)
                .toList();
        return Result.success(comments);
    }

    @Operation(summary = "发表评论")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/api/videos/{id}/comments")
    public Result<VideoCommentResponse> createComment(
            @PathVariable("id") Long videoId,
            @Valid @RequestBody CreateCommentRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        VideoComment comment = videoCommentService.create(videoId, request.content(), currentUser);
        return Result.success(toResponse(comment));
    }

    private VideoCommentResponse toResponse(VideoComment comment) {
        return new VideoCommentResponse(
                comment.getId(),
                comment.getVideoId(),
                comment.getUserId(),
                comment.getUsernameSnapshot(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
