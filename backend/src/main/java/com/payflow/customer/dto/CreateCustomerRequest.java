package com.payflow.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateCustomerRequest(
        @NotNull UUID merchantId,
        @NotBlank @Email String email,
        @NotBlank String fullName,
        String phone,
        String countryCode,
        Map<String, Object> metadata
) {}
