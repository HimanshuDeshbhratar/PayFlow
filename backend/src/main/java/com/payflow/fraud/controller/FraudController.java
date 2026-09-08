package com.payflow.fraud.controller;

import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.FraudAlertStatus;
import com.payflow.fraud.service.FraudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudService fraudService;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_ANALYST','MERCHANT_ADMIN')")
    public PageResponse<Map<String, Object>> alerts(
            @RequestParam(required = false) FraudAlertStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return fraudService.listAlerts(status, pageable);
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_ANALYST')")
    public List<Map<String, Object>> rules() {
        return fraudService.listRules();
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_ANALYST','MERCHANT_ADMIN')")
    public Map<String, Object> overview() {
        return fraudService.overview();
    }

    @PostMapping("/transactions/{transactionId}/mark-safe")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_ANALYST')")
    public Map<String, Object> markSafe(@PathVariable UUID transactionId) {
        return fraudService.markTransactionSafe(transactionId);
    }

    @PostMapping("/transactions/{transactionId}/block-customer")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_ANALYST')")
    public Map<String, Object> blockCustomer(
            @PathVariable UUID transactionId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : null;
        return fraudService.blockCustomerForTransaction(transactionId, reason);
    }
}
