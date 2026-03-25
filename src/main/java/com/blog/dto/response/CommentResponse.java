package com.blog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private UserResponse user;
    private Long postId;
    private Long parentId;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
    
    private Integer replyCount;
}
