package com.payflow.ledger.repository;

import com.payflow.common.enums.LedgerAccountType;
import com.payflow.ledger.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
    List<LedgerAccount> findByMerchantId(UUID merchantId);
    Optional<LedgerAccount> findByMerchantIdAndAccountType(UUID merchantId, LedgerAccountType accountType);
    Optional<LedgerAccount> findByCustomerIdAndAccountType(UUID customerId, LedgerAccountType accountType);
}
