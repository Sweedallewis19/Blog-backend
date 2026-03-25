package com.blog.service;

import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.CommentResponse;
import com.blog.entity.Comment;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.UnauthorizedException;
import com.blog.mapper.CommentMapper;
import com.blog.repository.CommentRepository;
import com.blog.repository.PostRepository;
import com.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse createComment(CommentRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Post post = postRepository.findByIdAndNotDeleted(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", request.getPostId()));

        Comment comment = commentMapper.toEntity(request);
        comment.setUser(user);
        comment.setPost(post);

        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentId()));
            comment.setParent(parentComment);
        }

        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findByIdWithUserAndReplies(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
        return buildCommentTree(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        List<Comment> topLevelComments = commentRepository.findTopLevelCommentsByPostId(postId);
        return topLevelComments.stream()
                .map(this::buildCommentTree)
                .toList();
    }

    @Transactional
    public void deleteComment(Long id, String userEmail) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this comment");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    private CommentResponse buildCommentTree(Comment comment) {
        CommentResponse response = commentMapper.toResponse(comment);
        
        List<Comment> replies = commentRepository.findRepliesByParentId(comment.getId());
        List<CommentResponse> replyResponses = new ArrayList<>();
        
        for (Comment reply : replies) {
            replyResponses.add(buildCommentTree(reply));
        }
        
        response.setReplies(replyResponses);
        response.setReplyCount(replyResponses.size());
        
        return response;
    }
}
