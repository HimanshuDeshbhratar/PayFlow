package com.payflow.customer.controller;

import com.payflow.common.dto.PageResponse;
import com.payflow.customer.dto.CreateCustomerRequest;
import com.payflow.customer.dto.CustomerResponse;
import com.payflow.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public PageResponse<CustomerResponse> list(
            @RequestParam(required = false) UUID merchantId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return customerService.list(merchantId, pageable);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return customerService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }
}
