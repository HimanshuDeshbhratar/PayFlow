package com.payflow.notification.service;

import com.payflow.common.dto.PageResponse;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.event.EventPublisher;
import com.payflow.notification.entity.Notification;
import com.payflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public Notification create(UUID userId, String type, String title, String message,
                               String resourceType, UUID resourceId) {
        Notification n = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .readFlag(false)
                .build());
        eventPublisher.publishNotificationCreated(n.getId(), null,
                EventPublisher.payload("userId", userId, "type", type, "title", title));
        return n;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listMine(Pageable pageable) {
        UUID userId = SecurityUtils.currentUser().getId();
        return PageResponse.from(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toMap));
    }

    @Transactional
    public Map<String, Object> markRead(UUID id) {
        UUID userId = SecurityUtils.currentUser().getId();
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> PayFlowException.notFound("Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw PayFlowException.forbidden("Access denied");
        }
        n.setReadFlag(true);
        notificationRepository.save(n);
        return toMap(n);
    }

    @Transactional
    public Map<String, Object> markAllRead() {
        UUID userId = SecurityUtils.currentUser().getId();
        int updated = notificationRepository.markAllRead(userId);
        return Map.of("updated", updated);
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("type", n.getType());
        m.put("title", n.getTitle());
        m.put("message", n.getMessage());
        m.put("resourceType", n.getResourceType());
        m.put("resourceId", n.getResourceId());
        m.put("read", n.isReadFlag());
        m.put("createdAt", n.getCreatedAt());
        return m;
    }
}
