package com.payflow.webhook.service;

import com.payflow.common.enums.WebhookDeliveryStatus;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.merchant.service.MerchantService;
import com.payflow.webhook.entity.Webhook;
import com.payflow.webhook.entity.WebhookDelivery;
import com.payflow.webhook.repository.WebhookDeliveryRepository;
import com.payflow.webhook.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final MerchantService merchantService;

    @Transactional
    public Map<String, Object> create(UUID merchantId, String url, List<String> events) {
        merchantService.assertCanAccess(merchantId);
        String secret = "whsec_" + UUID.randomUUID().toString().replace("-", "");
        Webhook webhook = webhookRepository.save(Webhook.builder()
                .merchantId(merchantId)
                .url(url)
                .events(events.toArray(String[]::new))
                .secretHash(sha256(secret))
                .active(true)
                .build());
        Map<String, Object> response = toMap(webhook);
        response.put("secret", secret);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID merchantId) {
        UUID mid = merchantId != null ? merchantId : SecurityUtils.currentUser().getMerchantId();
        if (mid == null) {
            throw PayFlowException.badRequest("merchantId required");
        }
        merchantService.assertCanAccess(mid);
        return webhookRepository.findByMerchantId(mid).stream().map(this::toMap).toList();
    }

    @Transactional
    public void dispatch(UUID merchantId, String eventType, Map<String, Object> payload) {
        List<Webhook> hooks = webhookRepository.findByMerchantIdAndActiveTrue(merchantId);
        for (Webhook hook : hooks) {
            if (hook.getEvents() == null || Arrays.stream(hook.getEvents()).noneMatch(e -> e.equals(eventType) || e.equals("*"))) {
                continue;
            }
            WebhookDelivery delivery = deliveryRepository.save(WebhookDelivery.builder()
                    .webhookId(hook.getId())
                    .eventType(eventType)
                    .payload(payload)
                    .status(WebhookDeliveryStatus.PENDING)
                    .attemptCount(0)
                    .build());
            attemptDelivery(delivery, hook);
        }
    }

    @Transactional
    public void attemptDelivery(WebhookDelivery delivery, Webhook webhook) {
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        // Simulated delivery — always succeeds for demo unless URL contains "fail"
        boolean fail = webhook.getUrl() != null && webhook.getUrl().contains("fail");
        if (fail && delivery.getAttemptCount() < 3) {
            delivery.setStatus(WebhookDeliveryStatus.RETRYING);
            delivery.setResponseCode(500);
            delivery.setResponseBody("Simulated failure");
            delivery.setNextRetryAt(Instant.now().plusSeconds(30L * delivery.getAttemptCount()));
        } else if (fail) {
            delivery.setStatus(WebhookDeliveryStatus.FAILED);
            delivery.setResponseCode(500);
            delivery.setResponseBody("Simulated permanent failure");
        } else {
            delivery.setStatus(WebhookDeliveryStatus.SUCCESS);
            delivery.setResponseCode(200);
            delivery.setResponseBody("{\"ok\":true}");
            delivery.setDeliveredAt(Instant.now());
        }
        deliveryRepository.save(delivery);
        log.info("Webhook delivery {} status={}", delivery.getId(), delivery.getStatus());
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void retryPending() {
        List<WebhookDelivery> retries = deliveryRepository
                .findByStatusAndNextRetryAtBefore(WebhookDeliveryStatus.RETRYING, Instant.now());
        for (WebhookDelivery delivery : retries) {
            webhookRepository.findById(delivery.getWebhookId()).ifPresent(hook -> attemptDelivery(delivery, hook));
        }
    }

    private Map<String, Object> toMap(Webhook w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("merchantId", w.getMerchantId());
        m.put("url", w.getUrl());
        m.put("events", w.getEvents());
        m.put("active", w.isActive());
        m.put("createdAt", w.getCreatedAt());
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
