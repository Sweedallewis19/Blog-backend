package com.blog.repository;

import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .name("Test User")
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
    void findByIdAndNotDeleted_shouldNotReturnDeleted() {
        Post post = postRepository.findBySlugWithUserAndComments("published-post").orElseThrow();

        post.setDeleted(true);
        postRepository.save(post);

        Optional<Post> result = postRepository.findByIdAndNotDeleted(post.getId());

        assertFalse(result.isPresent());
    }
}
