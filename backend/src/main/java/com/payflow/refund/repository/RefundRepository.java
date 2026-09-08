package com.payflow.refund.repository;

import com.payflow.common.enums.RefundStatus;
import com.payflow.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);
    List<Refund> findByPaymentId(UUID paymentId);

    @Query("SELECT COALESCE(SUM(r.amountCents), 0) FROM Refund r WHERE r.paymentId = :paymentId AND r.status <> :failed")
    long sumRefundedExcludingFailed(UUID paymentId, RefundStatus failed);
}
