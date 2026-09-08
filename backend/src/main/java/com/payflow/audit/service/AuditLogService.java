package com.payflow.audit.service;

import com.payflow.audit.entity.AuditLog;
import com.payflow.audit.repository.AuditLogRepository;
import com.payflow.common.dto.PageResponse;
import com.payflow.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String resourceType, String resourceId, String ip, Map<String, Object> metadata) {
        UUID userId = null;
        try {
            userId = SecurityUtils.currentUserOrNull() != null ? SecurityUtils.currentUserOrNull().getId() : null;
        } catch (Exception ignored) {
        }
        auditLogRepository.save(AuditLog.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ip)
                .metadata(metadata)
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(Pageable pageable) {
        return PageResponse.from(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toMap));
    }

    private Map<String, Object> toMap(AuditLog log) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", log.getId());
        m.put("userId", log.getUserId());
        m.put("action", log.getAction());
        m.put("resourceType", log.getResourceType());
        m.put("resourceId", log.getResourceId());
        m.put("ipAddress", log.getIpAddress());
        m.put("metadata", log.getMetadata());
        m.put("createdAt", log.getCreatedAt());
        return m;
    }
}
