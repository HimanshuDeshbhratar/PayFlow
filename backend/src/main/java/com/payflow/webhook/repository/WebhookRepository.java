package com.payflow.webhook.repository;

import com.payflow.webhook.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
    List<Webhook> findByMerchantIdAndActiveTrue(UUID merchantId);
    List<Webhook> findByMerchantId(UUID merchantId);
}
