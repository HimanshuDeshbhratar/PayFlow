package com.payflow.notification.controller;

import com.payflow.common.dto.PageResponse;
import com.payflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<Map<String, Object>> list(@PageableDefault(size = 20) Pageable pageable) {
        return notificationService.listMine(pageable);
    }

    @PutMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable UUID id) {
        return notificationService.markRead(id);
    }

    @PutMapping("/read-all")
    public Map<String, Object> markAllRead() {
        return notificationService.markAllRead();
    }
}
