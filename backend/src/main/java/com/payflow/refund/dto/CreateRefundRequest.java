package com.payflow.refund.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateRefundRequest(
        @NotNull @Min(1) Long amountCents,
        String reason
) {}
