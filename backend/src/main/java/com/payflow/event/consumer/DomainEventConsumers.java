package com.payflow.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.auth.repository.UserRepository;
import com.payflow.common.enums.UserRole;
import com.payflow.notification.service.NotificationService;
import com.payflow.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventConsumers {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final WebhookService webhookService;
    private final UserRepository userRepository;

    @KafkaListener(topics = "${payflow.kafka.topics.payment-events}", groupId = "payflow-notifications")
    public void onPaymentEvent(String message) {
        handleEvent(message, true);
    }

    @KafkaListener(topics = "${payflow.kafka.topics.fraud-events}", groupId = "payflow-notifications")
    public void onFraudEvent(String message) {
        handleEvent(message, true);
    }

    @KafkaListener(topics = "${payflow.kafka.topics.notification-events}", groupId = "payflow-notifications")
    public void onNotificationEvent(String message) {
        log.debug("Notification event received: {}", message);
    }

    @KafkaListener(topics = "${payflow.kafka.topics.settlement-events}", groupId = "payflow-webhooks")
    public void onSettlementEvent(String message) {
        handleEvent(message, false);
    }

    @KafkaListener(topics = "${payflow.kafka.topics.payment-events}", groupId = "payflow-webhooks")
    public void onPaymentWebhook(String message) {
        handleEvent(message, false);
    }

    private void handleEvent(String message, boolean createNotification) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.path("eventType").asText();
            UUID merchantId = node.hasNonNull("merchantId")
                    ? UUID.fromString(node.get("merchantId").asText())
                    : null;
            UUID resourceId = node.hasNonNull("resourceId")
                    ? UUID.fromString(node.get("resourceId").asText())
                    : null;

            Map<String, Object> payload = new HashMap<>();
            if (node.has("payload") && node.get("payload").isObject()) {
                payload = objectMapper.convertValue(node.get("payload"), Map.class);
            }

            if (createNotification && merchantId != null) {
                userRepository.findByMerchantId(merchantId).stream()
                        .filter(u -> u.getRole() == UserRole.MERCHANT_ADMIN || u.getRole() == UserRole.MERCHANT_USER)
                        .findFirst()
                        .ifPresent(user -> notificationService.create(
                                user.getId(),
                                eventType,
                                eventType,
                                "Event " + eventType + " for resource " + resourceId,
                                "payment",
                                resourceId
                        ));
            }

            if (merchantId != null) {
                webhookService.dispatch(merchantId, eventType, payload);
            }
        } catch (Exception ex) {
            log.warn("Failed to process domain event: {}", ex.getMessage());
        }
    }
}
