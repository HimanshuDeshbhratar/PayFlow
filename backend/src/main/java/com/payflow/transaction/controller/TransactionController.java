package com.payflow.transaction.controller;

import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return transactionService.list(status, pageable);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        return transactionService.getDetail(id);
    }
}
