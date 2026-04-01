package com.blog.controller;

import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.CommentResponse;
import com.blog.dto.response.UserResponse;
import com.blog.service.CommentService;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private CommentResponse commentResponse;

    @BeforeEach
    void setUp() {
        commentResponse = CommentResponse.builder()
                .id(1L)
                .content("Test comment")
                .user(UserResponse.builder().id(1L).name("Test User").build())
                .postId(1L)
                .createdAt(LocalDateTime.now())
                .replies(Collections.emptyList())
                .replyCount(0)
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createComment_shouldReturn201() throws Exception {
        CommentRequest request = CommentRequest.builder()
                .content("New comment")
                .postId(1L)
                .build();

        when(commentService.createComment(any(CommentRequest.class), eq("test@example.com")))
                .thenReturn(commentResponse);

        mockMvc.perform(post("/api/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("Test comment"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteComment_shouldReturn200ForOwner() throws Exception {
        doNothing().when(commentService).deleteComment(1L, "test@example.com");

        mockMvc.perform(delete("/api/comments/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
