package com.payflow.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMerchantRequest(
        @NotBlank @Size(max = 255) String businessName,
        String legalName,
        @Size(min = 2, max = 3) String countryCode,
        @Size(min = 3, max = 3) String currency
) {}
