package com.payflow.merchant.dto;

import com.payflow.common.enums.MerchantSettlementStatus;
import com.payflow.common.enums.MerchantStatus;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String businessName,
        String legalName,
        MerchantStatus status,
        String countryCode,
        String currency,
        MerchantSettlementStatus settlementStatus,
        Instant createdAt
) {}
