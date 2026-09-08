package com.payflow.auth.dto;

import com.payflow.common.enums.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        UUID merchantId,
        boolean active
) {}
