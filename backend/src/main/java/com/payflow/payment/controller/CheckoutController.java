package com.payflow.payment.controller;

import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public Map<String, Object> get(@PathVariable UUID paymentId) {
        return paymentService.checkoutGet(paymentId);
    }

    @PostMapping("/{paymentId}")
    public PaymentResponse confirm(
            @PathVariable UUID paymentId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return paymentService.checkoutConfirm(paymentId, body != null ? body : Map.of());
    }
}
