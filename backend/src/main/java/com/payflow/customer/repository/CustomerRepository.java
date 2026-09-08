package com.payflow.customer.repository;

import com.payflow.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Page<Customer> findByMerchantId(UUID merchantId, Pageable pageable);
    Optional<Customer> findByMerchantIdAndEmailIgnoreCase(UUID merchantId, String email);
    long countByMerchantId(UUID merchantId);
    boolean existsByMerchantIdAndEmailIgnoreCase(UUID merchantId, String email);
}
