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

import java.util.List;
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
    private Comment topLevelComment;
    private Comment replyComment;

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

        topLevelComment = commentRepository.save(Comment.builder()
                .content("Top level comment")
                .user(testUser)
                .post(testPost)
                .deleted(false)
                .build());

        replyComment = commentRepository.save(Comment.builder()
                .content("Reply comment")
                .user(testUser)
                .post(testPost)
                .parent(topLevelComment)
                .deleted(false)
                .build());

        commentRepository.save(Comment.builder()
                .content("Deleted comment")
                .user(testUser)
                .post(testPost)
                .deleted(true)
                .build());
    }

    @Test
    void findTopLevelCommentsByPostId_shouldReturnOnlyTopLevel() {
        List<Comment> result = commentRepository.findTopLevelCommentsByPostId(testPost.getId());

        assertEquals(1, result.size());
        assertEquals("Top level comment", result.get(0).getContent());
    }

    @Test
    void findRepliesByParentId_shouldReturnReplies() {
        List<Comment> result = commentRepository.findRepliesByParentId(topLevelComment.getId());

        assertEquals(1, result.size());
        assertEquals("Reply comment", result.get(0).getContent());
    }

    @Test
    void findRepliesByParentId_shouldReturnEmptyForNoReplies() {
        List<Comment> result = commentRepository.findRepliesByParentId(replyComment.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdWithUserAndReplies_shouldReturnCommentWithUser() {
        Optional<Comment> result = commentRepository.findByIdWithUserAndReplies(topLevelComment.getId());

        assertTrue(result.isPresent());
        assertNotNull(result.get().getUser());
        assertEquals("test@example.com", result.get().getUser().getEmail());
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

    @Test
    void countByPostId_shouldReturnCorrectCount() {
        Long count = commentRepository.countByPostId(testPost.getId());

        assertEquals(2L, count);
    }

    @Test
    void findAllByPostId_shouldReturnAllNonDeleted() {
        List<Comment> result = commentRepository.findAllByPostId(testPost.getId());

        assertEquals(2, result.size());
    }
}
