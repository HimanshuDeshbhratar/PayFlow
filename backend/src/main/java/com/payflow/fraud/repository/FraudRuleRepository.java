package com.payflow.fraud.repository;

import com.payflow.fraud.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {
    List<FraudRule> findByEnabledTrue();
    Optional<FraudRule> findByCode(String code);
}
