package com.payflow.api.service;

import com.payflow.api.entity.ApiKey;
import com.payflow.api.repository.ApiKeyRepository;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final MerchantService merchantService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Map<String, Object> create(UUID merchantId, String name) {
        merchantService.assertCanAccess(merchantId);
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        String secret = "pf_live_" + HexFormat.of().formatHex(bytes);
        String prefix = secret.substring(0, Math.min(12, secret.length()));

        ApiKey key = apiKeyRepository.save(ApiKey.builder()
                .merchantId(merchantId)
                .name(name)
                .keyPrefix(prefix)
                .keyHash(sha256(secret))
                .createdBy(SecurityUtils.currentUser().getId())
                .build());

        Map<String, Object> response = toMap(key);
        response.put("secret", secret);
        response.put("message", "Store this secret now; it will not be shown again");
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID merchantId) {
        UUID mid = merchantId != null ? merchantId : SecurityUtils.currentUser().getMerchantId();
        if (mid == null) {
            throw PayFlowException.badRequest("merchantId required");
        }
        merchantService.assertCanAccess(mid);
        return apiKeyRepository.findByMerchantId(mid).stream().map(this::toMap).toList();
    }

    @Transactional
    public Map<String, Object> revoke(UUID id) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> PayFlowException.notFound("API key not found"));
        merchantService.assertCanAccess(key.getMerchantId());
        key.setRevokedAt(Instant.now());
        apiKeyRepository.save(key);
        return toMap(key);
    }

    private Map<String, Object> toMap(ApiKey key) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", key.getId());
        m.put("merchantId", key.getMerchantId());
        m.put("name", key.getName());
        m.put("keyPrefix", key.getKeyPrefix());
        m.put("revokedAt", key.getRevokedAt());
        m.put("lastUsedAt", key.getLastUsedAt());
        m.put("createdAt", key.getCreatedAt());
        return m;
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
