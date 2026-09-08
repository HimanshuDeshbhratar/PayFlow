package com.payflow.payment.controller;

import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.service.PaymentService;
import com.payflow.refund.dto.CreateRefundRequest;
import com.payflow.refund.dto.RefundResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return paymentService.createPayment(request, idempotencyKey);
    }

    @GetMapping
    public PageResponse<PaymentResponse> list(
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return paymentService.list(status, pageable);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return paymentService.get(id);
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancel(@PathVariable UUID id) {
        return paymentService.cancel(id);
    }

    @PostMapping("/{id}/refund")
    public RefundResponse refund(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRefundRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return paymentService.refund(id, request, idempotencyKey);
    }
}
