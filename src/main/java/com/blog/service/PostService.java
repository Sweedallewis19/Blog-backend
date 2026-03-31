package com.blog.service;

import com.blog.dto.request.PostRequest;
import com.blog.dto.response.PageResponse;
import com.blog.dto.response.PostResponse;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
import com.blog.mapper.PostMapper;
import com.blog.repository.PostRepository;
import com.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    @Transactional
    public PostResponse createPost(PostRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Post post = postMapper.toEntity(request);
        post.setUser(user);
        post.setSlug(generateUniqueSlug(post.getSlug()));

        if (request.getPublished() == null) {
            post.setPublished(true);
        }

        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }

    @Transactional
    public PostResponse updatePost(String slug, PostRequest request, String userEmail) {
        Post post = postRepository.findBySlugAndNotDeleted(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this post");
        }

        postMapper.updateEntity(post, request);
        if (request.getTitle() != null) {
            post.setSlug(generateUniqueSlug(postMapper.generateSlug(request.getTitle())));
        }
        Post updatedPost = postRepository.save(post);
        return postMapper.toResponse(updatedPost);
    }

    @Transactional
    public void deletePost(String slug, String userEmail) {
        Post post = postRepository.findBySlugAndNotDeleted(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this post");
        }

        post.setDeleted(true);
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostBySlug(String slug, String currentUserEmail) {
        Post post = postRepository.findBySlugWithUserAndComments(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));

        if (!post.getPublished()) {
            if (currentUserEmail == null || !post.getUser().getEmail().equals(currentUserEmail)) {
                throw new ResourceNotFoundException("Post", "slug", slug);
            }
        }

        return postMapper.toResponse(post);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id, String currentUserEmail) {
        Post post = postRepository.findByIdWithUserAndComments(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        if (!post.getPublished()) {
            if (currentUserEmail == null || !post.getUser().getEmail().equals(currentUserEmail)) {
                throw new ResourceNotFoundException("Post", "id", id);
            }
        }

        return postMapper.toResponse(post);
    }

    @Transactional
    public PostResponse updatePostById(Long id, PostRequest request, String userEmail) {
        Post post = postRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this post");
        }

        postMapper.updateEntity(post, request);
        if (request.getTitle() != null) {
            post.setSlug(generateUniqueSlug(postMapper.generateSlug(request.getTitle())));
        }
        Post updatedPost = postRepository.save(post);
        return postMapper.toResponse(updatedPost);
    }

    @Transactional
    public void deletePostById(Long id, String userEmail) {
        Post post = postRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this post");
        }

        post.setDeleted(true);
        postRepository.save(post);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        Post post = postRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getAllPosts(Pageable pageable, String currentUserEmail) {
        Long userId = null;
        if (currentUserEmail != null) {
            User user = userRepository.findByEmail(currentUserEmail).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        Page<Post> postPage = postRepository.findAllPublishedOrOwnedByUser(userId, pageable);
        List<PostResponse> posts = postPage.getContent().stream()
                .map(postMapper::toResponse)
                .toList();

        return PageResponse.<PostResponse>builder()
                .content(posts)
                .pageNumber(postPage.getNumber())
                .pageSize(postPage.getSize())
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .first(postPage.isFirst())
                .last(postPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPostsByUser(Long userId, Pageable pageable, String currentUserEmail) {
        Long viewerId = null;
        if (currentUserEmail != null) {
            User viewer = userRepository.findByEmail(currentUserEmail).orElse(null);
            if (viewer != null && viewer.getId().equals(userId)) {
                viewerId = viewer.getId();
            }
        }

        Page<Post> postPage = postRepository.findByUserIdWithVisibility(userId, viewerId, pageable);
        List<PostResponse> posts = postPage.getContent().stream()
                .map(postMapper::toResponse)
                .toList();

        return PageResponse.<PostResponse>builder()
                .content(posts)
                .pageNumber(postPage.getNumber())
                .pageSize(postPage.getSize())
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .first(postPage.isFirst())
                .last(postPage.isLast())
                .build();
    }

    @Transactional
    public void incrementViewCountBySlug(String slug) {
        Post post = postRepository.findBySlugAndNotDeleted(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }

    private String generateUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;
        while (postRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }
}
