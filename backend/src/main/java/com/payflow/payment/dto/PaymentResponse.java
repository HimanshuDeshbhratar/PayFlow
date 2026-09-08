package com.payflow.payment.dto;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.enums.RiskLevel;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID merchantId,
        UUID customerId,
        String reference,
        long amountCents,
        String currency,
        PaymentStatus status,
        String description,
        String checkoutSessionId,
        Integer riskScore,
        RiskLevel riskLevel,
        UUID transactionId,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {}
