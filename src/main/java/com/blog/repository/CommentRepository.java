package com.blog.repository;

import com.blog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    void deleteByUserId(Long userId);
    
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL AND c.deleted = false ORDER BY c.createdAt DESC")
    List<Comment> findTopLevelCommentsByPostId(@Param("postId") Long postId);
    
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.replies WHERE c.id = :id AND c.deleted = false")
    Optional<Comment> findByIdWithUserAndReplies(@Param("id") Long id);
    
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.deleted = false ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostId(@Param("postId") Long postId);
    
    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId AND c.deleted = false")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.deleted = false")
    Long countByPostId(@Param("postId") Long postId);
}
