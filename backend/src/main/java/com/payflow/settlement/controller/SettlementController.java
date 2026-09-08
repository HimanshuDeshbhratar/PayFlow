package com.payflow.settlement.controller;

import com.payflow.common.dto.PageResponse;
import com.payflow.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(required = false) UUID merchantId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return settlementService.list(merchantId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT_ADMIN')")
    public Map<String, Object> create(@RequestParam UUID merchantId) {
        return settlementService.createForMerchant(merchantId);
    }
}
