package com.payflow.transaction.repository;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.enums.RiskLevel;
import com.payflow.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByMerchantId(UUID merchantId, Pageable pageable);
    Page<Transaction> findByMerchantIdAndStatus(UUID merchantId, PaymentStatus status, Pageable pageable);
    Page<Transaction> findByStatus(PaymentStatus status, Pageable pageable);
    Optional<Transaction> findByPaymentId(UUID paymentId);
    List<Transaction> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.customerId = :customerId AND t.merchantId = :merchantId AND t.createdAt >= :since")
    long countByCustomerAndMerchantSince(UUID customerId, UUID merchantId, Instant since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.customerId = :customerId AND t.merchantId = :merchantId")
    long countByCustomerAndMerchant(UUID customerId, UUID merchantId);

    long countByStatus(PaymentStatus status);
    long countByRiskLevel(RiskLevel riskLevel);

    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM Transaction t WHERE t.status = :status AND t.createdAt >= :since")
    long sumAmountByStatusSince(PaymentStatus status, Instant since);

    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM Transaction t WHERE t.merchantId = :merchantId AND t.status = :status")
    long sumAmountByMerchantAndStatus(UUID merchantId, PaymentStatus status);

    List<Transaction> findTop10ByOrderByCreatedAtDesc();
    List<Transaction> findTop10ByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
