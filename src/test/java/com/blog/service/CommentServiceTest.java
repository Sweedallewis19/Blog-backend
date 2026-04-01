package com.blog.service;

import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.CommentResponse;
import com.blog.entity.Comment;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.enums.Role;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
import com.blog.mapper.CommentMapper;
import com.blog.repository.CommentRepository;
import com.blog.repository.PostRepository;
import com.blog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private User otherUser;
    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .password("password")
                .roles(Set.of(Role.ROLE_USER))
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .name("Other User")
                .password("password")
                .roles(Set.of(Role.ROLE_USER))
                .build();

        testPost = Post.builder()
                .id(1L)
                .title("Test Post")
                .slug("test-post")
                .content("Post content")
                .published(true)
                .user(testUser)
                .viewCount(0L)
                .deleted(false)
                .build();

        testComment = Comment.builder()
                .id(1L)
                .content("Test comment")
                .user(testUser)
                .post(testPost)
                .deleted(false)
                .build();
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost() != null ? comment.getPost().getId() : null)
                .build();
    }

    @Test
    void createCommentBySlug_shouldCreateComment() {
        CommentRequest request = CommentRequest.builder()
                .content("New comment")
                .build();

        Comment entity = Comment.builder().content("New comment").deleted(false).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(postRepository.findBySlugAndNotDeleted("test-post")).thenReturn(Optional.of(testPost));
        when(commentMapper.toEntity(request)).thenReturn(entity);
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));
        when(commentMapper.toResponse(any(Comment.class))).thenAnswer(i -> toResponse(i.getArgument(0)));

        CommentResponse response = commentService.createCommentBySlug("test-post", request, "test@example.com");

        assertNotNull(response);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createCommentBySlug_shouldThrowForNonExistentPost() {
        CommentRequest request = CommentRequest.builder()
                .content("New comment")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(postRepository.findBySlugAndNotDeleted("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.createCommentBySlug("non-existent", request, "test@example.com"));
    }

    @Test
    void createCommentBySlug_shouldThrowForDraftPostToNonOwner() {
        CommentRequest request = CommentRequest.builder()
                .content("New comment")
                .build();

        Post draftPost = Post.builder()
                .id(3L)
                .title("Draft Post")
                .slug("draft-post")
                .published(false)
                .user(testUser)
                .deleted(false)
                .build();

        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(postRepository.findBySlugAndNotDeleted("draft-post")).thenReturn(Optional.of(draftPost));

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.createCommentBySlug("draft-post", request, "other@example.com"));
    }

    @Test
    void getCommentsByPostSlug_shouldReturnComments() {
        when(postRepository.findBySlugAndNotDeleted("test-post")).thenReturn(Optional.of(testPost));
        when(commentRepository.findTopLevelCommentsByPostId(1L))
                .thenReturn(Collections.singletonList(testComment));
        when(commentRepository.findRepliesByParentId(1L)).thenReturn(Collections.emptyList());
        when(commentMapper.toResponse(testComment)).thenReturn(toResponse(testComment));

        var responses = commentService.getCommentsByPostSlug("test-post");

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void deleteComment_shouldAllowOwnerToDelete() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        commentService.deleteComment(1L, "test@example.com");

        assertTrue(testComment.getDeleted());
        verify(commentRepository).save(testComment);
    }

    @Test
    void deleteComment_shouldThrowForNonOwner() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedException.class,
                () -> commentService.deleteComment(1L, "other@example.com"));
    }
}
