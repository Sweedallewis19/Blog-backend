package com.blog.service;

import com.blog.dto.request.LoginRequest;
import com.blog.dto.request.RegisterRequest;
import com.blog.dto.response.AuthResponse;
import com.blog.dto.response.UserResponse;
import com.blog.entity.User;
import com.blog.enums.Role;
import com.blog.exception.BadRequestException;
import com.blog.exception.UnauthorizedException;
import com.blog.exception.ResourceNotFoundException;
import com.blog.mapper.UserMapper;
import com.blog.repository.UserRepository;
import com.blog.repository.PostRepository;
import com.blog.repository.CommentRepository;
import com.blog.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.ROLE_USER));

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(userMapper.toResponse(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(userMapper.toResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        postRepository.deleteByUserId(user.getId());
        commentRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }
}
