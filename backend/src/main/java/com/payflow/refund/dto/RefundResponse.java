package com.payflow.refund.dto;

import com.payflow.common.enums.RefundStatus;

import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        UUID transactionId,
        UUID merchantId,
        long amountCents,
        String currency,
        RefundStatus status,
        String reason,
        Instant createdAt
) {}
