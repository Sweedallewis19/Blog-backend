package com.blog.mapper;

import com.blog.dto.request.RegisterRequest;
import com.blog.dto.response.UserResponse;
import com.blog.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        
        return User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .bio(request.getBio())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
