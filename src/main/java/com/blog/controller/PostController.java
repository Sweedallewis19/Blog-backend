package com.blog.controller;

import com.blog.dto.request.PostRequest;
import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.ApiResponse;
import com.blog.dto.response.PageResponse;
import com.blog.dto.response.PostResponse;
import com.blog.dto.response.CommentResponse;
import com.blog.service.PostService;
import com.blog.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody PostRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        PostResponse response = postService.createPost(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        String currentUserEmail = authentication != null ? authentication.getName() : null;
        PageResponse<PostResponse> response = postService.getAllPosts(pageable, currentUserEmail);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostBySlug(
            @PathVariable String slug,
            Authentication authentication
    ) {
        String currentUserEmail = authentication != null ? authentication.getName() : null;
        PostResponse response = postService.getPostBySlug(slug, currentUserEmail);
        postService.incrementViewCountBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String currentUserEmail = authentication != null ? authentication.getName() : null;
        PostResponse response = postService.getPostById(id, currentUserEmail);
        postService.incrementViewCount(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPostsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size);
        String currentUserEmail = authentication != null ? authentication.getName() : null;
        PageResponse<PostResponse> response = postService.getPostsByUser(userId, pageable, currentUserEmail);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable String slug,
            @Valid @RequestBody PostRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        PostResponse response = postService.updatePost(slug, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Post updated successfully", response));
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePostById(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        PostResponse response = postService.updatePostById(id, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Post updated successfully", response));
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable String slug,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        postService.deletePost(slug, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePostById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        postService.deletePostById(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }

    @PostMapping("/{slug}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createCommentBySlug(
            @PathVariable String slug,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        CommentResponse response = commentService.createCommentBySlug(slug, request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment created successfully", response));
    }

    @GetMapping("/{slug}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getCommentsByPostSlug(@PathVariable String slug) {
        List<CommentResponse> responses = commentService.getCommentsByPostSlug(slug);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
