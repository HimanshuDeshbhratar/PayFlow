package com.payflow.api.repository;

import com.payflow.api.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByMerchantIdAndRevokedAtIsNull(UUID merchantId);
    List<ApiKey> findByMerchantId(UUID merchantId);
}
