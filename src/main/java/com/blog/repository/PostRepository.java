package com.blog.repository;

import com.blog.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    void deleteByUserId(Long userId);
    
    Page<Post> findByDeletedFalse(Pageable pageable);
    
    Page<Post> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.deleted = false AND p.published = true")
    Page<Post> findAllPublished(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.deleted = false AND (p.published = true OR (:userId IS NOT NULL AND p.user.id = :userId))")
    Page<Post> findAllPublishedOrOwnedByUser(@Param("userId") Long userId, Pageable pageable);

    Page<Post> findByUserIdAndDeletedFalseAndPublishedTrue(Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.deleted = false AND (p.published = true OR (:userId IS NOT NULL AND p.user.id = :userId)) AND p.user.id = :profileUserId")
    Page<Post> findByUserIdWithVisibility(@Param("profileUserId") Long profileUserId, @Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deleted = false")
    Optional<Post> findByIdAndNotDeleted(@Param("id") Long id);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.comments WHERE p.id = :id AND p.deleted = false")
    Optional<Post> findByIdWithUserAndComments(@Param("id") Long id);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user WHERE p.slug = :slug AND p.deleted = false")
    Optional<Post> findBySlugWithUser(@Param("slug") String slug);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.comments WHERE p.slug = :slug AND p.deleted = false")
    Optional<Post> findBySlugWithUserAndComments(@Param("slug") String slug);

    @Query("SELECT p FROM Post p WHERE p.slug = :slug AND p.deleted = false")
    Optional<Post> findBySlugAndNotDeleted(@Param("slug") String slug);

    boolean existsBySlug(String slug);
}
