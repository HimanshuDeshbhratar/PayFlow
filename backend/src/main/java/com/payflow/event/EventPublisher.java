package com.payflow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.config.PayFlowProperties;
import com.payflow.event.dto.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PayFlowProperties properties;
    private final ObjectMapper objectMapper;

    public void publishPaymentCreated(UUID paymentId, UUID merchantId, Map<String, Object> payload) {
        publish(properties.getKafka().getTopics().getPaymentEvents(),
                DomainEvent.of("PaymentCreated", paymentId, merchantId, payload));
    }

    public void publishPaymentApproved(UUID paymentId, UUID merchantId, Map<String, Object> payload) {
        publish(properties.getKafka().getTopics().getPaymentEvents(),
                DomainEvent.of("PaymentApproved", paymentId, merchantId, payload));
    }

    public void publishPaymentBlocked(UUID paymentId, UUID merchantId, Map<String, Object> payload) {
        publish(properties.getKafka().getTopics().getPaymentEvents(),
                DomainEvent.of("PaymentBlocked", paymentId, merchantId, payload));
        publish(properties.getKafka().getTopics().getFraudEvents(),
                DomainEvent.of("FraudDetected", paymentId, merchantId, payload));
    }

    public void publishPaymentCompleted(UUID paymentId, UUID merchantId, Map<String, Object> payload) {
        publish(properties.getKafka().getTopics().getPaymentEvents(),
                DomainEvent.of("PaymentCompleted", paymentId, merchantId, payload));
    }

    public void publishPaymentRefunded(UUID paymentId, UUID merchantId, Map<String, Object> payload) {
        publish(properties.getKafka().getTopics().getPaymentEvents(),
                DomainEvent.of("PaymentRefunded", paymentId, merchantId, payload));
    }

    public void publishFraudDetected(UUID resourceId, UUID merchantId, Map<String, Object> payload) {
        publish(properties.getKafka().getTopics().getFraudEvents(),
                DomainEvent.of("FraudDetected", resourceId, merchantId, payload));
    }

    public void publishSettlementCompleted(UUID settlementId, UUID merchantId, Map<String, Object> payload) {
        publish(properties.getKafka().getTopics().getSettlementEvents(),
                DomainEvent.of("SettlementCompleted", settlementId, merchantId, payload));
    }

    public void publishNotificationCreated(UUID notificationId, UUID merchantId, Map<String, Object> payload) {
        publish(properties.getKafka().getTopics().getNotificationEvents(),
                DomainEvent.of("NotificationCreated", notificationId, merchantId, payload));
    }

    private void publish(String topic, DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.resourceId() != null ? event.resourceId().toString() : null, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}: {}", event.eventType(), e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to publish event {} to {}: {}", event.eventType(), topic, e.getMessage());
        }
    }

    public static Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
