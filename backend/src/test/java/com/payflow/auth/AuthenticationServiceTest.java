package com.payflow.auth;

import com.payflow.auth.dto.LoginRequest;
import com.payflow.auth.dto.RegisterRequest;
import com.payflow.auth.entity.RefreshToken;
import com.payflow.auth.entity.User;
import com.payflow.auth.repository.RefreshTokenRepository;
import com.payflow.auth.repository.UserRepository;
import com.payflow.auth.security.JwtService;
import com.payflow.auth.service.AuthService;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.PayFlowException;
import com.payflow.config.PayFlowProperties;
import com.payflow.redis.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RateLimitService rateLimitService;

    AuthService authService;
    PayFlowProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PayFlowProperties();
        properties.getJwt().setAccessTokenExpiryMs(900_000);
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtService, properties, rateLimitService);
    }

    @Test
    void registerCreatesUser() {
        when(userRepository.existsByEmailIgnoreCase("new@payflow.demo")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.createAccessToken(any())).thenReturn("access");
        when(jwtService.createRefreshToken(any())).thenReturn("refresh");
        when(jwtService.extractExpiry(anyString())).thenReturn(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.register(new RegisterRequest(
                "new@payflow.demo", "password123", "New User", UserRole.MERCHANT_USER));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.user().email()).isEqualTo("new@payflow.demo");
        assertThat(response.user().role()).isEqualTo(UserRole.MERCHANT_USER);
    }

    @Test
    void loginRateLimited() {
        when(rateLimitService.tryLogin("1.2.3.4")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "x"), "1.2.3.4"))
                .isInstanceOf(PayFlowException.class)
                .hasMessageContaining("Too many");
    }

    @Test
    void loginSuccess() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("a@b.com")
                .passwordHash("hash")
                .fullName("A")
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        when(rateLimitService.tryLogin(anyString())).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.createAccessToken(any())).thenReturn("access");
        when(jwtService.createRefreshToken(any())).thenReturn("refresh");
        when(jwtService.extractExpiry(anyString())).thenReturn(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.login(new LoginRequest("a@b.com", "secret"), "127.0.0.1");
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }
}
