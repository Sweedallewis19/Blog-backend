package com.blog.mapper;

import com.blog.dto.request.CommentRequest;
import com.blog.dto.response.CommentResponse;
import com.blog.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final UserMapper userMapper;

    public Comment toEntity(CommentRequest request) {
        if (request == null) {
            return null;
        }
        
        return Comment.builder()
                .content(request.getContent())
                .deleted(false)
                .build();
    }

    public CommentResponse toResponse(Comment comment) {
        if (comment == null) {
            return null;
        }
        
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(userMapper.toResponse(comment.getUser()))
                .postId(comment.getPost() != null ? comment.getPost().getId() : null)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .replyCount(comment.getReplies() != null ? comment.getReplies().size() : 0)
                .replies(new ArrayList<>())
                .build();
    }

    public List<CommentResponse> toResponseList(List<Comment> comments) {
        if (comments == null) {
            return null;
        }
        
        return comments.stream()
                .map(this::toResponse)
                .toList();
    }
}
