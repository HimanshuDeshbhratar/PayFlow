package com.payflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.auth.controller.AuthController;
import com.payflow.auth.dto.AuthResponse;
import com.payflow.auth.dto.LoginRequest;
import com.payflow.auth.dto.UserResponse;
import com.payflow.auth.service.AuthService;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService authService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsTokens() throws Exception {
        UserResponse user = new UserResponse(UUID.randomUUID(), "admin@payflow.demo", "Admin",
                UserRole.ADMIN, null, true);
        when(authService.login(any(LoginRequest.class), anyString()))
                .thenReturn(AuthResponse.of("access-token", "refresh-token", 900000, user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@payflow.demo", "PayFlowAdmin!2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.email").value("admin@payflow.demo"));
    }
}
