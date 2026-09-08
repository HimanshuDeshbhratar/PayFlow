package com.payflow.fraud.repository;

import com.payflow.common.enums.FraudAlertStatus;
import com.payflow.fraud.entity.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
    Page<FraudAlert> findByMerchantId(UUID merchantId, Pageable pageable);
    Page<FraudAlert> findByStatus(FraudAlertStatus status, Pageable pageable);
    long countByStatus(FraudAlertStatus status);
    List<FraudAlert> findTop5ByOrderByCreatedAtDesc();
    List<FraudAlert> findTop5ByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
