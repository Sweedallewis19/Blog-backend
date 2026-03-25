package com.blog.controller;

import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.ApiResponse;
import com.blog.dto.response.CommentResponse;
import com.blog.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        CommentResponse response = commentService.createComment(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentById(@PathVariable Long id) {
        CommentResponse response = commentService.getCommentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentResponse> responses = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        commentService.deleteComment(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }
}
