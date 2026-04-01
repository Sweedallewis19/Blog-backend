package com.blog.repository;

import com.blog.entity.Comment;
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
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password")
                .roles(Set.of(Role.ROLE_USER))
                .build());

        testPost = postRepository.save(Post.builder()
                .title("Test Post")
                .slug("test-post")
                .content("Test content")
                .published(true)
                .user(testUser)
                .viewCount(0L)
                .deleted(false)
                .build());
    }

    @Test
    void findByIdWithUserAndReplies_shouldNotReturnDeleted() {
        Comment deletedComment = commentRepository.save(Comment.builder()
                .content("To be deleted")
                .user(testUser)
                .post(testPost)
                .deleted(false)
                .build());

        deletedComment.setDeleted(true);
        commentRepository.save(deletedComment);

        Optional<Comment> result = commentRepository.findByIdWithUserAndReplies(deletedComment.getId());

        assertFalse(result.isPresent());
    }
}
