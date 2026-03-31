    package com.blog.controller;
    
    import com.blog.dto.request.LoginRequest;
    import com.blog.dto.request.RegisterRequest;
    import com.blog.dto.response.AuthResponse;
    import com.blog.dto.response.UserResponse;
    import com.blog.exception.BadRequestException;
    import com.blog.exception.UnauthorizedException;
    import com.blog.service.AuthService;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
    import org.springframework.boot.test.context.SpringBootTest;
    import org.springframework.boot.test.mock.mockito.MockBean;
    import org.springframework.http.MediaType;
    import org.springframework.security.test.context.support.WithMockUser;
    import org.springframework.test.context.ActiveProfiles;
    import org.springframework.test.web.servlet.MockMvc;
    
    import static org.mockito.ArgumentMatchers.*;
    import static org.mockito.Mockito.*;
    import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
    
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    class AuthControllerTest {
    
        @Autowired
        private MockMvc mockMvc;
    
        @MockBean
        private AuthService authService;
    
        @Autowired
        private ObjectMapper objectMapper;
    
        private AuthResponse authResponse;
    
        @BeforeEach
        void setUp() {
            authResponse = AuthResponse.builder()
                    .token("test-jwt-token")
                    .type("Bearer")
                    .user(UserResponse.builder().id(1L).name("Test User").build())
                    .build();
        }
    
        @Test
        void register_shouldReturn201ForValidRequest() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Test User")
                    .email("test@example.com")
                    .password("password123")
                    .build();
    
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);
    
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("test-jwt-token"));
        }
    
        @Test
        void register_shouldReturn400ForDuplicateEmail() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Test User")
                    .email("existing@example.com")
                    .password("password123")
                    .build();
    
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new BadRequestException("Email already exists"));
    
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    
        @Test
        void register_shouldReturn400ForInvalidData() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("")
                    .email("invalid")
                    .password("123")
                    .build();
    
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    
        @Test
        void login_shouldReturn200ForValidCredentials() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();
    
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);
    
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("test-jwt-token"));
        }
    
        @Test
        void login_shouldReturn401ForInvalidCredentials() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("wrongpassword")
                    .build();
    
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Invalid email or password"));
    
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    
        @Test
        @WithMockUser(username = "test@example.com")
        void getCurrentUser_shouldReturn200WithAuth() throws Exception {
            UserResponse userResponse = UserResponse.builder().id(1L).name("Test User").build();
            when(authService.getCurrentUser("test@example.com")).thenReturn(userResponse);
    
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Test User"));
        }
    
        @Test
        void getCurrentUser_shouldReturnErrorWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().is5xxServerError());
        }
    
        @Test
        @WithMockUser(username = "test@example.com")
        void deleteCurrentUser_shouldReturn200() throws Exception {
            doNothing().when(authService).deleteUser("test@example.com");
    
            mockMvc.perform(delete("/api/auth/me")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
