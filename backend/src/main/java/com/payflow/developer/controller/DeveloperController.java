package com.payflow.developer.controller;

import com.payflow.api.service.ApiKeyService;
import com.payflow.common.security.SecurityUtils;
import com.payflow.webhook.service.WebhookService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/developer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MERCHANT_ADMIN','MERCHANT_USER')")
public class DeveloperController {

    private final ApiKeyService apiKeyService;
    private final WebhookService webhookService;

    public record CreateApiKeyRequest(UUID merchantId, @NotBlank String name) {}
    public record CreateWebhookRequest(@NotNull UUID merchantId, @NotBlank String url, @NotEmpty List<String> events) {}

    @GetMapping("/api-keys")
    public List<Map<String, Object>> listKeys(@RequestParam(required = false) UUID merchantId) {
        return apiKeyService.list(merchantId);
    }

    @PostMapping("/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createKey(@RequestBody CreateApiKeyRequest request) {
        UUID merchantId = request.merchantId() != null ? request.merchantId() : SecurityUtils.currentUser().getMerchantId();
        return apiKeyService.create(merchantId, request.name());
    }

    @PostMapping("/api-keys/{id}/revoke")
    public Map<String, Object> revokeKey(@PathVariable UUID id) {
        return apiKeyService.revoke(id);
    }

    @GetMapping("/webhooks")
    public List<Map<String, Object>> listWebhooks(@RequestParam(required = false) UUID merchantId) {
        return webhookService.list(merchantId);
    }

    @PostMapping("/webhooks")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createWebhook(@RequestBody CreateWebhookRequest request) {
        return webhookService.create(request.merchantId(), request.url(), request.events());
    }
}
