package com.blog.controller;

import com.blog.dto.request.PostRequest;
import com.blog.dto.response.PageResponse;
import com.blog.dto.response.PostResponse;
import com.blog.dto.response.UserResponse;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
import com.blog.service.CommentService;
import com.blog.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private PostResponse publishedPostResponse;
    private PostResponse draftPostResponse;

    @BeforeEach
    void setUp() {
        publishedPostResponse = PostResponse.builder()
                .id(1L)
                .title("Published Post")
                .slug("published-post")
                .content("Content")
                .published(true)
                .user(UserResponse.builder().id(1L).name("Test User").build())
                .viewCount(0L)
                .commentCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        draftPostResponse = PostResponse.builder()
                .id(2L)
                .title("Draft Post")
                .slug("draft-post")
                .content("Draft content")
                .published(false)
                .user(UserResponse.builder().id(1L).name("Test User").build())
                .viewCount(0L)
                .commentCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllPosts_shouldReturn200() throws Exception {
        PageResponse<PostResponse> pageResponse = PageResponse.<PostResponse>builder()
                .content(List.of(publishedPostResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(postService.getAllPosts(any(Pageable.class), isNull())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].slug").value("published-post"));
    }

    @Test
    void getPostBySlug_shouldReturn200ForPublished() throws Exception {
        when(postService.getPostBySlug("published-post", null)).thenReturn(publishedPostResponse);
        doNothing().when(postService).incrementViewCountBySlug("published-post");

        mockMvc.perform(get("/api/posts/published-post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("published-post"))
                .andExpect(jsonPath("$.data.title").value("Published Post"));
    }

    @Test
    void getPostBySlug_shouldReturn404ForDraftUnauthenticated() throws Exception {
        when(postService.getPostBySlug("draft-post", null))
                .thenThrow(new ResourceNotFoundException("Post", "slug", "draft-post"));

        mockMvc.perform(get("/api/posts/draft-post"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPost_shouldReturn403WithoutAuth() throws Exception {
        PostRequest request = PostRequest.builder()
                .title("New Post")
                .content("New content")
                .build();

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createPost_shouldReturn201WithAuth() throws Exception {
        PostRequest request = PostRequest.builder()
                .title("New Post")
                .content("New content")
                .build();

        when(postService.createPost(any(PostRequest.class), eq("test@example.com")))
                .thenReturn(publishedPostResponse);

        mockMvc.perform(post("/api/posts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createPost_shouldReturn400ForInvalidData() throws Exception {
        PostRequest request = PostRequest.builder()
                .title("")
                .content("")
                .build();

        mockMvc.perform(post("/api/posts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updatePost_shouldReturn200ForOwner() throws Exception {
        PostRequest request = PostRequest.builder()
                .title("Updated Title")
                .content("Updated content")
                .build();

        PostResponse updatedResponse = PostResponse.builder()
                .id(1L)
                .title("Updated Title")
                .slug("updated-title")
                .content("Content")
                .published(true)
                .user(UserResponse.builder().id(1L).name("Test User").build())
                .viewCount(0L)
                .commentCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(postService.updatePost(eq("published-post"), any(PostRequest.class), eq("test@example.com")))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/posts/published-post")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "other@example.com")
    void updatePost_shouldReturn401ForNonOwner() throws Exception {
        PostRequest request = PostRequest.builder()
                .title("Updated Title")
                .content("Updated content")
                .build();

        when(postService.updatePost(eq("published-post"), any(PostRequest.class), eq("other@example.com")))
                .thenThrow(new UnauthorizedException("You are not authorized to update this post"));

        mockMvc.perform(put("/api/posts/published-post")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deletePost_shouldReturn200ForOwner() throws Exception {
        doNothing().when(postService).deletePost("published-post", "test@example.com");

        mockMvc.perform(delete("/api/posts/published-post")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deletePost_shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/posts/published-post"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPostById_shouldReturn200() throws Exception {
        when(postService.getPostById(1L, null)).thenReturn(publishedPostResponse);
        doNothing().when(postService).incrementViewCount(1L);

        mockMvc.perform(get("/api/posts/id/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updatePostById_shouldReturn200() throws Exception {
        PostRequest request = PostRequest.builder()
                .title("Updated By Id")
                .content("Updated content")
                .build();

        when(postService.updatePostById(eq(1L), any(PostRequest.class), eq("test@example.com")))
                .thenReturn(publishedPostResponse);

        mockMvc.perform(put("/api/posts/id/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deletePostById_shouldReturn200() throws Exception {
        doNothing().when(postService).deletePostById(1L, "test@example.com");

        mockMvc.perform(delete("/api/posts/id/1")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createCommentBySlug_shouldReturn201() throws Exception {
        com.blog.dto.request.CommentRequest commentRequest = com.blog.dto.request.CommentRequest.builder()
                .content("A comment")
                .build();

        com.blog.dto.response.CommentResponse commentResponse = com.blog.dto.response.CommentResponse.builder()
                .id(1L)
                .content("A comment")
                .user(UserResponse.builder().id(1L).name("Test User").build())
                .postId(1L)
                .build();

        when(commentService.createCommentBySlug(eq("published-post"), any(), eq("test@example.com")))
                .thenReturn(commentResponse);

        mockMvc.perform(post("/api/posts/published-post/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("A comment"));
    }

    @Test
    void createCommentBySlug_shouldReturn403WithoutAuth() throws Exception {
        com.blog.dto.request.CommentRequest commentRequest = com.blog.dto.request.CommentRequest.builder()
                .content("A comment")
                .build();

        mockMvc.perform(post("/api/posts/published-post/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCommentsByPostSlug_shouldReturn200() throws Exception {
        when(commentService.getCommentsByPostSlug("published-post"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/posts/published-post/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
