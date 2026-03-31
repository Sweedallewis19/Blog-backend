package com.blog.repository;

import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password")
                .roles(Set.of(Role.ROLE_USER))
                .build());

        otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .name("Other User")
                .password("password")
                .roles(Set.of(Role.ROLE_USER))
                .build());

        postRepository.save(Post.builder()
                .title("Published Post")
                .slug("published-post")
                .content("Published content")
                .published(true)
                .user(testUser)
                .viewCount(0L)
                .deleted(false)
                .build());

        postRepository.save(Post.builder()
                .title("Draft Post")
                .slug("draft-post")
                .content("Draft content")
                .published(false)
                .user(testUser)
                .viewCount(0L)
                .deleted(false)
                .build());

        postRepository.save(Post.builder()
                .title("Other User Published")
                .slug("other-user-published")
                .content("Other content")
                .published(true)
                .user(otherUser)
                .viewCount(0L)
                .deleted(false)
                .build());

        postRepository.save(Post.builder()
                .title("Deleted Post")
                .slug("deleted-post")
                .content("Deleted content")
                .published(true)
                .user(testUser)
                .viewCount(0L)
                .deleted(true)
                .build());
    }

    @Test
    void findAllPublishedOrOwnedByUser_shouldReturnPublishedAndOwnDrafts() {
        Page<Post> result = postRepository.findAllPublishedOrOwnedByUser(
                testUser.getId(), PageRequest.of(0, 10));

        assertEquals(3, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getSlug().equals("published-post")));
        assertTrue(result.getContent().stream().anyMatch(p -> p.getSlug().equals("draft-post")));
        assertTrue(result.getContent().stream().anyMatch(p -> p.getSlug().equals("other-user-published")));
        assertFalse(result.getContent().stream().anyMatch(p -> p.getSlug().equals("deleted-post")));
    }

    @Test
    void findAllPublishedOrOwnedByUser_shouldReturnOnlyPublishedForNullUser() {
        Page<Post> result = postRepository.findAllPublishedOrOwnedByUser(
                null, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(Post::getPublished));
    }

    @Test
    void findBySlugWithUserAndComments_shouldReturnPost() {
        Optional<Post> result = postRepository.findBySlugWithUserAndComments("published-post");

        assertTrue(result.isPresent());
        assertEquals("Published Post", result.get().getTitle());
        assertNotNull(result.get().getUser());
    }

    @Test
    void findBySlugWithUserAndComments_shouldNotReturnDeleted() {
        Optional<Post> result = postRepository.findBySlugWithUserAndComments("deleted-post");

        assertFalse(result.isPresent());
    }

    @Test
    void findBySlugAndNotDeleted_shouldReturnExisting() {
        Optional<Post> result = postRepository.findBySlugAndNotDeleted("published-post");

        assertTrue(result.isPresent());
    }

    @Test
    void findBySlugAndNotDeleted_shouldReturnEmptyForNonExistent() {
        Optional<Post> result = postRepository.findBySlugAndNotDeleted("non-existent");

        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndNotDeleted_shouldReturnExisting() {
        Post post = postRepository.findBySlugAndNotDeleted("published-post").orElseThrow();

        Optional<Post> result = postRepository.findByIdAndNotDeleted(post.getId());

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdAndNotDeleted_shouldNotReturnDeleted() {
        Post post = postRepository.findBySlugWithUserAndComments("published-post").orElseThrow();

        post.setDeleted(true);
        postRepository.save(post);

        Optional<Post> result = postRepository.findByIdAndNotDeleted(post.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void findByUserIdWithVisibility_shouldReturnPublishedAndOwnDraftsForOwner() {
        Page<Post> result = postRepository.findByUserIdWithVisibility(
                testUser.getId(), testUser.getId(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getSlug().equals("published-post")));
        assertTrue(result.getContent().stream().anyMatch(p -> p.getSlug().equals("draft-post")));
    }

    @Test
    void findByUserIdWithVisibility_shouldReturnOnlyPublishedForOtherUser() {
        Page<Post> result = postRepository.findByUserIdWithVisibility(
                testUser.getId(), otherUser.getId(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("published-post", result.getContent().get(0).getSlug());
    }

    @Test
    void existsBySlug_shouldReturnTrueForExisting() {
        assertTrue(postRepository.existsBySlug("published-post"));
    }

    @Test
    void existsBySlug_shouldReturnFalseForNonExistent() {
        assertFalse(postRepository.existsBySlug("non-existent-slug"));
    }
}
