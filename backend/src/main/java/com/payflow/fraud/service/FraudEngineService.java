package com.payflow.fraud.service;

import com.payflow.common.enums.RiskDecision;
import com.payflow.common.enums.RiskLevel;
import com.payflow.config.PayFlowProperties;
import com.payflow.fraud.entity.FraudAlert;
import com.payflow.fraud.entity.FraudRule;
import com.payflow.fraud.entity.RiskAssessment;
import com.payflow.fraud.repository.FraudAlertRepository;
import com.payflow.fraud.repository.FraudRuleRepository;
import com.payflow.fraud.repository.RiskAssessmentRepository;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FraudEngineService {

    private final FraudRuleRepository fraudRuleRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final PayFlowProperties properties;

    public record FraudResult(int score, RiskLevel level, RiskDecision decision, List<Map<String, Object>> factors) {}

    @Transactional
    public FraudResult assessAndPersist(Transaction tx) {
        FraudResult result = calculate(tx);

        RiskAssessment assessment = RiskAssessment.builder()
                .transactionId(tx.getId())
                .riskScore(result.score())
                .riskLevel(result.level())
                .decision(result.decision())
                .factors(result.factors())
                .assessedAt(Instant.now())
                .build();
        riskAssessmentRepository.save(assessment);

        tx.setRiskScore(result.score());
        tx.setRiskLevel(result.level());
        transactionRepository.save(tx);

        if (result.decision() == RiskDecision.BLOCK || result.decision() == RiskDecision.REVIEW) {
            FraudAlert alert = FraudAlert.builder()
                    .transactionId(tx.getId())
                    .merchantId(tx.getMerchantId())
                    .severity(result.level())
                    .title("Fraud screening: " + result.decision())
                    .description("Risk score " + result.score() + " — " + result.level())
                    .status(com.payflow.common.enums.FraudAlertStatus.OPEN)
                    .build();
            fraudAlertRepository.save(alert);
        }

        return result;
    }

    public FraudResult calculate(Transaction tx) {
        Map<String, FraudRule> rules = new HashMap<>();
        for (FraudRule rule : fraudRuleRepository.findByEnabledTrue()) {
            rules.put(rule.getCode(), rule);
        }

        List<Map<String, Object>> factors = new ArrayList<>();
        int score = 0;

        score += apply(rules, factors, "HIGH_AMOUNT", 30,
                tx.getAmountCents() >= properties.getFraud().getHighAmountCents());

        score += apply(rules, factors, "NEW_DEVICE", 20, tx.isNewDevice());

        score += apply(rules, factors, "UNUSUAL_LOCATION", 20, isUnusualLocation(tx));

        boolean velocityHit = false;
        if (tx.getCustomerId() != null) {
            Instant since = Instant.now().minus(properties.getFraud().getVelocityWindowMinutes(), ChronoUnit.MINUTES);
            long count = transactionRepository.countByCustomerAndMerchantSince(
                    tx.getCustomerId(), tx.getMerchantId(), since);
            velocityHit = count >= properties.getFraud().getVelocityMaxCount();
        }
        score += apply(rules, factors, "VELOCITY", 15, velocityHit);

        boolean firstMerchant = false;
        if (tx.getCustomerId() != null) {
            long prior = transactionRepository.countByCustomerAndMerchant(tx.getCustomerId(), tx.getMerchantId());
            firstMerchant = prior <= 1;
        }
        score += apply(rules, factors, "FIRST_MERCHANT", 10, firstMerchant);

        score += apply(rules, factors, "SUSPICIOUS_IP", 25, isSuspiciousIp(tx.getIpAddress()));

        RiskLevel level = toLevel(score);
        RiskDecision decision;
        if (score >= properties.getFraud().getBlockThreshold()) {
            decision = RiskDecision.BLOCK;
        } else if (score >= properties.getFraud().getReviewThreshold()) {
            decision = RiskDecision.REVIEW;
        } else {
            decision = RiskDecision.APPROVE;
        }

        return new FraudResult(score, level, decision, factors);
    }

    public static RiskLevel toLevel(int score) {
        if (score >= 80) return RiskLevel.CRITICAL;
        if (score >= 60) return RiskLevel.HIGH;
        if (score >= 30) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private int apply(Map<String, FraudRule> rules, List<Map<String, Object>> factors,
                      String code, int defaultWeight, boolean triggered) {
        if (!triggered) {
            return 0;
        }
        FraudRule rule = rules.get(code);
        int weight = rule != null ? rule.getWeight() : defaultWeight;
        Map<String, Object> factor = new LinkedHashMap<>();
        factor.put("code", code);
        factor.put("weight", weight);
        factor.put("triggered", true);
        if (rule != null) {
            factor.put("name", rule.getName());
        }
        factors.add(factor);
        return weight;
    }

    private boolean isUnusualLocation(Transaction tx) {
        if (tx.getLocationCountry() == null) {
            return false;
        }
        String country = tx.getLocationCountry().toUpperCase(Locale.ROOT);
        return Set.of("RU", "KP", "IR", "SY", "NG").contains(country)
                || "UNKNOWN".equalsIgnoreCase(tx.getLocationCity());
    }

    private boolean isSuspiciousIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return ip.startsWith("10.255.") || ip.startsWith("192.0.2.") || ip.contains("suspicious");
    }
}
