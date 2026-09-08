package com.payflow.webhook.repository;

import com.payflow.common.enums.WebhookDeliveryStatus;
import com.payflow.webhook.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(WebhookDeliveryStatus status, Instant time);
}
