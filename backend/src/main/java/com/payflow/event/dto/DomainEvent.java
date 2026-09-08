package com.payflow.event.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        String eventType,
        UUID resourceId,
        UUID merchantId,
        Instant occurredAt,
        Map<String, Object> payload
) {
    public static DomainEvent of(String eventType, UUID resourceId, UUID merchantId, Map<String, Object> payload) {
        return new DomainEvent(eventType, resourceId, merchantId, Instant.now(), payload);
    }
}
