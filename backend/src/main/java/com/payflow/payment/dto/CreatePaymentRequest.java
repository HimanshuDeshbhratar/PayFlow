package com.payflow.payment.dto;

import com.payflow.common.enums.PaymentMethodType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID merchantId,
        UUID customerId,
        @NotNull @Min(1) Long amountCents,
        String currency,
        String description,
        PaymentMethodType paymentMethodType,
        String locationCity,
        String locationCountry,
        String deviceInfo,
        String ipAddress,
        Boolean newDevice,
        Map<String, Object> metadata
) {}
