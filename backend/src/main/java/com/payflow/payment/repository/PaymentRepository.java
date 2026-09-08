package com.payflow.payment.repository;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByMerchantId(UUID merchantId, Pageable pageable);
    Page<Payment> findByMerchantIdAndStatus(UUID merchantId, PaymentStatus status, Pageable pageable);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    Optional<Payment> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);
    Optional<Payment> findByCheckoutSessionId(String checkoutSessionId);
    long countByMerchantIdAndStatus(UUID merchantId, PaymentStatus status);
}
