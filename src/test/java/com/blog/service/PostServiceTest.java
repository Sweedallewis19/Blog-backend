package com.blog.service;

import com.blog.dto.request.PostRequest;
import com.blog.dto.response.PostResponse;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.enums.Role;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
import com.blog.mapper.PostMapper;
import com.blog.repository.PostRepository;
import com.blog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private User otherUser;
    private Post publishedPost;
    private Post draftPost;

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

        publishedPost = Post.builder()
                .id(1L)
                .title("My Published Post")
                .slug("my-published-post")
                .content("Content here")
                .published(true)
                .user(testUser)
                .viewCount(0L)
                .deleted(false)
                .build();

        draftPost = Post.builder()
                .id(2L)
                .title("My Draft Post")
                .slug("my-draft-post")
                .content("Draft content")
                .published(false)
                .user(testUser)
                .viewCount(0L)
                .deleted(false)
                .build();
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .published(post.getPublished())
                .viewCount(post.getViewCount())
                .build();
    }

    @Test
    void createPost_shouldGenerateSlug() {
        PostRequest request = PostRequest.builder()
                .title("Hello World")
                .content("Some content")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(postMapper.toEntity(request)).thenReturn(Post.builder()
                .title("Hello World").slug("hello-world").content("Some content")
                .published(true).viewCount(0L).deleted(false).build());
        when(postMapper.generateSlug("Hello World")).thenReturn("hello-world");
        when(postRepository.existsBySlug(anyString())).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));
        when(postMapper.toResponse(any(Post.class))).thenAnswer(i -> toResponse(i.getArgument(0)));

        PostResponse response = postService.createPost(request, "test@example.com");

        assertEquals("hello-world", response.getSlug());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void getPostBySlug_shouldReturnPublishedPost() {
        when(postRepository.findBySlugWithUserAndComments("my-published-post"))
                .thenReturn(Optional.of(publishedPost));
        when(postMapper.toResponse(publishedPost)).thenReturn(toResponse(publishedPost));

        PostResponse response = postService.getPostBySlug("my-published-post", null);

        assertNotNull(response);
        assertEquals("my-published-post", response.getSlug());
    }

    @Test
    void getPostBySlug_shouldThrowForDraftToOtherUser() {
        when(postRepository.findBySlugWithUserAndComments("my-draft-post"))
                .thenReturn(Optional.of(draftPost));

        assertThrows(ResourceNotFoundException.class,
                () -> postService.getPostBySlug("my-draft-post", "other@example.com"));
    }

    @Test
    void getPostBySlug_shouldThrowForNonExistentPost() {
        when(postRepository.findBySlugWithUserAndComments("non-existent"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.getPostBySlug("non-existent", null));
    }

    @Test
    void updatePost_shouldAllowOwnerToUpdate() {
        PostRequest request = PostRequest.builder()
                .title("Updated Title")
                .content("Updated content")
                .build();

        when(postRepository.findBySlugAndNotDeleted("my-published-post"))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(postMapper.generateSlug("Updated Title")).thenReturn("updated-title");
        when(postRepository.existsBySlug("updated-title")).thenReturn(false);
        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            PostRequest req = invocation.getArgument(1);
            post.setTitle(req.getTitle());
            post.setSlug("updated-title");
            return null;
        }).when(postMapper).updateEntity(any(Post.class), any(PostRequest.class));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));
        when(postMapper.toResponse(any(Post.class))).thenAnswer(i -> toResponse(i.getArgument(0)));

        PostResponse response = postService.updatePost("my-published-post", request, "test@example.com");

        assertEquals("Updated Title", response.getTitle());
        assertEquals("updated-title", response.getSlug());
    }

    @Test
    void updatePost_shouldThrowForNonOwner() {
        PostRequest request = PostRequest.builder()
                .title("Updated Title")
                .content("Updated content")
                .build();

        when(postRepository.findBySlugAndNotDeleted("my-published-post"))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedException.class,
                () -> postService.updatePost("my-published-post", request, "other@example.com"));
    }

    @Test
    void deletePost_shouldAllowOwnerToDelete() {
        when(postRepository.findBySlugAndNotDeleted("my-published-post"))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        postService.deletePost("my-published-post", "test@example.com");

        assertTrue(publishedPost.getDeleted());
        verify(postRepository).save(publishedPost);
    }
}
