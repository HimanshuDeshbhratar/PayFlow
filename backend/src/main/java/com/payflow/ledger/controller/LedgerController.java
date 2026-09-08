package com.payflow.ledger.controller;

import com.payflow.common.dto.PageResponse;
import com.payflow.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/accounts")
    public List<Map<String, Object>> accounts(@RequestParam(required = false) UUID merchantId) {
        return ledgerService.listAccounts(merchantId);
    }

    @GetMapping("/accounts/{id}/entries")
    public PageResponse<Map<String, Object>> entries(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ledgerService.listEntries(id, pageable);
    }
}
