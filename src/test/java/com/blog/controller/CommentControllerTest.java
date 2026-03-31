package com.blog.controller;

import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.CommentResponse;
import com.blog.dto.response.UserResponse;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
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
import java.util.List;

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
    void createComment_shouldReturn403WithoutAuth() throws Exception {
        CommentRequest request = CommentRequest.builder()
                .content("New comment")
                .postId(1L)
                .build();

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createComment_shouldReturn400ForEmptyContent() throws Exception {
        CommentRequest request = CommentRequest.builder()
                .content("")
                .postId(1L)
                .build();

        mockMvc.perform(post("/api/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCommentById_shouldReturn200() throws Exception {
        when(commentService.getCommentById(1L)).thenReturn(commentResponse);

        mockMvc.perform(get("/api/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.content").value("Test comment"));
    }

    @Test
    void getCommentById_shouldReturn404ForNonExistent() throws Exception {
        when(commentService.getCommentById(999L))
                .thenThrow(new ResourceNotFoundException("Comment", "id", 999L));

        mockMvc.perform(get("/api/comments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCommentsByPostId_shouldReturn200() throws Exception {
        when(commentService.getCommentsByPostId(1L))
                .thenReturn(List.of(commentResponse));

        mockMvc.perform(get("/api/comments/post/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("Test comment"));
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

    @Test
    void deleteComment_shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "other@example.com")
    void deleteComment_shouldReturn401ForNonOwner() throws Exception {
        doThrow(new UnauthorizedException("You are not authorized to delete this comment"))
                .when(commentService).deleteComment(1L, "other@example.com");

        mockMvc.perform(delete("/api/comments/1")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
