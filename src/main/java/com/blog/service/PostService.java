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

        if (request.getPublished() == null) {
            post.setPublished(true);
        }

        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request, String userEmail) {
        Post post = postRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this post");
        }

        postMapper.updateEntity(post, request);
        Post updatedPost = postRepository.save(post);
        return postMapper.toResponse(updatedPost);
    }

    @Transactional
    public void deletePost(Long id, String userEmail) {
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

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findByIdWithUserAndComments(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        return postMapper.toResponse(post);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getAllPosts(Pageable pageable) {
        Page<Post> postPage = postRepository.findByDeletedFalse(pageable);
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
    public PageResponse<PostResponse> getPostsByUser(Long userId, Pageable pageable) {
        Page<Post> postPage = postRepository.findByUserIdAndDeletedFalse(userId, pageable);
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
    public void incrementViewCount(Long id) {
        Post post = postRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }
}
