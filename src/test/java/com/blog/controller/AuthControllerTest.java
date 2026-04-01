    package com.blog.controller;

    import com.blog.dto.request.LoginRequest;
    import com.blog.dto.request.RegisterRequest;
    import com.blog.dto.response.AuthResponse;
    import com.blog.dto.response.UserResponse;
    import com.blog.service.AuthService;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
    import org.springframework.boot.test.context.SpringBootTest;
    import org.springframework.boot.test.mock.mockito.MockBean;
    import org.springframework.http.MediaType;
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
    }
