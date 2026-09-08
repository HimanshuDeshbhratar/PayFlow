package com.payflow.auth.service;

import com.payflow.auth.dto.*;
import com.payflow.auth.entity.RefreshToken;
import com.payflow.auth.entity.User;
import com.payflow.auth.repository.RefreshTokenRepository;
import com.payflow.auth.repository.UserRepository;
import com.payflow.auth.security.JwtService;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.PayFlowException;
import com.payflow.config.PayFlowProperties;
import com.payflow.redis.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PayFlowProperties properties;
    private final RateLimitService rateLimitService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw PayFlowException.conflict("Email already registered");
        }
        if (request.role() == UserRole.ADMIN) {
            throw PayFlowException.forbidden("Cannot self-register as ADMIN");
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(request.role())
                .active(true)
                .build();
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        if (!rateLimitService.tryLogin(clientIp)) {
            throw PayFlowException.tooManyRequests("Too many login attempts. Try again later.");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> PayFlowException.unauthorized("Invalid credentials"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw PayFlowException.unauthorized("Invalid credentials");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw PayFlowException.unauthorized("Invalid refresh token");
        }

        String hash = sha256(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> PayFlowException.unauthorized("Refresh token revoked or unknown"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw PayFlowException.unauthorized("Refresh token expired");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> PayFlowException.unauthorized("User not found"));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            String hash = sha256(refreshToken);
            refreshTokenRepository.findByTokenHashAndRevokedFalse(hash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        } catch (Exception ignored) {
            // best-effort logout
        }
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getMerchantId(),
                user.isActive()
        );
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.createAccessToken(user);
        String refresh = jwtService.createRefreshToken(user);

        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(refresh))
                .expiresAt(jwtService.extractExpiry(refresh))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);

        return AuthResponse.of(access, refresh, properties.getJwt().getAccessTokenExpiryMs(), toUserResponse(user));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
