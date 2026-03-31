package com.blog.mapper;

import com.blog.dto.request.PostRequest;
import com.blog.dto.response.PostResponse;
import com.blog.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final UserMapper userMapper;

    public Post toEntity(PostRequest request) {
        if (request == null) {
            return null;
        }

        return Post.builder()
                .title(request.getTitle())
                .slug(generateSlug(request.getTitle()))
                .content(request.getContent())
                .published(request.getPublished() != null ? request.getPublished() : true)
                .viewCount(0L)
                .deleted(false)
                .build();
    }

    public PostResponse toResponse(Post post) {
        if (post == null) {
            return null;
        }

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .published(post.getPublished())
                .user(userMapper.toResponse(post.getUser()))
                .viewCount(post.getViewCount())
                .commentCount(post.getComments() != null ? (long) post.getComments().size() : 0L)
                .createdAt(post.getCreatedAt())
                .build();
    }

    public void updateEntity(Post post, PostRequest request) {
        if (request == null || post == null) {
            return;
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
            post.setSlug(generateSlug(request.getTitle()));
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getPublished() != null) {
            post.setPublished(request.getPublished());
        }
    }

    public String generateSlug(String title) {
        if (title == null) {
            return null;
        }
        String slug = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug;
    }
}
