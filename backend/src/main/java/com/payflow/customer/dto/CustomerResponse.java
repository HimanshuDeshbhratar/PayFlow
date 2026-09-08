package com.payflow.customer.dto;

import com.payflow.common.enums.CustomerStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UUID merchantId,
        String email,
        String fullName,
        String phone,
        String countryCode,
        CustomerStatus status,
        boolean blocked,
        String blockedReason,
        Map<String, Object> metadata,
        Instant createdAt
) {}
