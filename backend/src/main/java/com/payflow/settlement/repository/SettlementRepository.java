package com.payflow.settlement.repository;

import com.payflow.settlement.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    Page<Settlement> findByMerchantId(UUID merchantId, Pageable pageable);
}
