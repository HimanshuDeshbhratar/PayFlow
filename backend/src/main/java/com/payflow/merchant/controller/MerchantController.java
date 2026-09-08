package com.payflow.merchant.controller;

import com.payflow.common.dto.PageResponse;
import com.payflow.merchant.dto.CreateMerchantRequest;
import com.payflow.merchant.dto.MerchantResponse;
import com.payflow.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping
    public PageResponse<MerchantResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return merchantService.list(pageable);
    }

    @GetMapping("/{id}")
    public MerchantResponse get(@PathVariable UUID id) {
        return merchantService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MerchantResponse create(@Valid @RequestBody CreateMerchantRequest request) {
        return merchantService.create(request);
    }
}
