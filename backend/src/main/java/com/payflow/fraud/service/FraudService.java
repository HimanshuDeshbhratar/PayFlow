package com.payflow.fraud.service;

import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.FraudAlertStatus;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.customer.service.CustomerService;
import com.payflow.fraud.entity.FraudAlert;
import com.payflow.fraud.entity.FraudRule;
import com.payflow.fraud.repository.FraudAlertRepository;
import com.payflow.fraud.repository.FraudRuleRepository;
import com.payflow.merchant.service.MerchantService;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FraudService {

    private final FraudAlertRepository alertRepository;
    private final FraudRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerService customerService;
    private final MerchantService merchantService;

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listAlerts(FraudAlertStatus status, Pageable pageable) {
        var user = SecurityUtils.currentUser();
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RISK_ANALYST) {
            var page = status != null
                    ? alertRepository.findByStatus(status, pageable)
                    : alertRepository.findAll(pageable);
            return PageResponse.from(page.map(this::toAlert));
        }
        UUID merchantId = Optional.ofNullable(user.getMerchantId())
                .orElseThrow(() -> PayFlowException.forbidden("No merchant association"));
        var page = alertRepository.findByMerchantId(merchantId, pageable);
        if (status != null) {
            page = new org.springframework.data.domain.PageImpl<>(
                    page.getContent().stream().filter(a -> a.getStatus() == status).toList(),
                    pageable,
                    page.getTotalElements()
            );
        }
        return PageResponse.from(page.map(this::toAlert));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRules() {
        return ruleRepository.findAll().stream().map(this::toRule).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("openAlerts", alertRepository.countByStatus(FraudAlertStatus.OPEN));
        overview.put("investigating", alertRepository.countByStatus(FraudAlertStatus.INVESTIGATING));
        overview.put("resolved", alertRepository.countByStatus(FraudAlertStatus.RESOLVED));
        overview.put("enabledRules", ruleRepository.findByEnabledTrue().size());
        overview.put("recentAlerts", alertRepository.findTop5ByOrderByCreatedAtDesc().stream().map(this::toAlert).toList());
        return overview;
    }

    @Transactional
    public Map<String, Object> markTransactionSafe(UUID transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> PayFlowException.notFound("Transaction not found"));
        merchantService.assertCanAccess(tx.getMerchantId());
        if (tx.getCustomerId() != null) {
            customerService.markSafe(tx.getCustomerId());
        }
        alertRepository.findAll().stream()
                .filter(a -> a.getTransactionId().equals(transactionId) && a.getStatus() == FraudAlertStatus.OPEN)
                .forEach(a -> {
                    a.setStatus(FraudAlertStatus.DISMISSED);
                    a.setResolvedAt(Instant.now());
                    alertRepository.save(a);
                });
        return Map.of("transactionId", transactionId, "action", "MARKED_SAFE");
    }

    @Transactional
    public Map<String, Object> blockCustomerForTransaction(UUID transactionId, String reason) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> PayFlowException.notFound("Transaction not found"));
        merchantService.assertCanAccess(tx.getMerchantId());
        if (tx.getCustomerId() == null) {
            throw PayFlowException.badRequest("Transaction has no customer");
        }
        customerService.blockCustomer(tx.getCustomerId(), reason != null ? reason : "Blocked by risk analyst");
        alertRepository.findAll().stream()
                .filter(a -> a.getTransactionId().equals(transactionId) && a.getStatus() == FraudAlertStatus.OPEN)
                .forEach(a -> {
                    a.setStatus(FraudAlertStatus.RESOLVED);
                    a.setResolvedAt(Instant.now());
                    alertRepository.save(a);
                });
        return Map.of("transactionId", transactionId, "action", "CUSTOMER_BLOCKED");
    }

    private Map<String, Object> toAlert(FraudAlert a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("transactionId", a.getTransactionId());
        m.put("merchantId", a.getMerchantId());
        m.put("severity", a.getSeverity());
        m.put("title", a.getTitle());
        m.put("description", a.getDescription());
        m.put("status", a.getStatus());
        m.put("createdAt", a.getCreatedAt());
        m.put("resolvedAt", a.getResolvedAt());
        return m;
    }

    private Map<String, Object> toRule(FraudRule r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("code", r.getCode());
        m.put("name", r.getName());
        m.put("description", r.getDescription());
        m.put("weight", r.getWeight());
        m.put("thresholdValue", r.getThresholdValue());
        m.put("enabled", r.isEnabled());
        return m;
    }
}
