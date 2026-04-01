package com.blog.controller;

import com.blog.dto.request.PostRequest;
import com.blog.dto.response.PostResponse;
import com.blog.dto.response.UserResponse;
import com.blog.service.CommentService;
import com.blog.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

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
}
