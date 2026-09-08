package com.payflow.transaction.service;

import com.payflow.auth.security.UserPrincipal;
import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.customer.entity.Customer;
import com.payflow.customer.repository.CustomerRepository;
import com.payflow.fraud.entity.RiskAssessment;
import com.payflow.fraud.repository.RiskAssessmentRepository;
import com.payflow.merchant.service.MerchantService;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.entity.TransactionEvent;
import com.payflow.transaction.repository.TransactionEventRepository;
import com.payflow.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final CustomerRepository customerRepository;
    private final MerchantService merchantService;

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(PaymentStatus status, Pageable pageable) {
        UserPrincipal user = SecurityUtils.currentUser();
        Page<Transaction> page;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RISK_ANALYST) {
            page = status != null
                    ? transactionRepository.findByStatus(status, pageable)
                    : transactionRepository.findAll(pageable);
        } else {
            UUID merchantId = user.getMerchantId();
            if (merchantId == null) {
                throw PayFlowException.forbidden("No merchant association");
            }
            page = status != null
                    ? transactionRepository.findByMerchantIdAndStatus(merchantId, status, pageable)
                    : transactionRepository.findByMerchantId(merchantId, pageable);
        }
        return PageResponse.from(page.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDetail(UUID id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> PayFlowException.notFound("Transaction not found"));
        merchantService.assertCanAccess(tx.getMerchantId());

        Map<String, Object> detail = new LinkedHashMap<>(toSummary(tx));

        riskAssessmentRepository.findByTransactionId(tx.getId()).ifPresent(ra ->
                detail.put("riskAssessment", toRisk(ra)));

        List<TransactionEvent> timeline = eventRepository.findByTransactionIdOrderByCreatedAtAsc(tx.getId());
        detail.put("timeline", timeline.stream().map(this::toEvent).toList());

        if (tx.getCustomerId() != null) {
            Customer customer = customerRepository.findById(tx.getCustomerId()).orElse(null);
            if (customer != null) {
                Map<String, Object> customerInfo = new LinkedHashMap<>();
                customerInfo.put("id", customer.getId());
                customerInfo.put("email", customer.getEmail());
                customerInfo.put("fullName", customer.getFullName());
                customerInfo.put("blocked", customer.isBlocked());
                customerInfo.put("status", customer.getStatus());
                detail.put("customer", customerInfo);
            }
            List<Transaction> activity = transactionRepository
                    .findByCustomerIdOrderByCreatedAtDesc(tx.getCustomerId(), PageRequest.of(0, 10));
            detail.put("customerActivity", activity.stream().map(this::toSummary).toList());
        }

        return detail;
    }

    private Map<String, Object> toSummary(Transaction tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", tx.getId());
        m.put("paymentId", tx.getPaymentId());
        m.put("merchantId", tx.getMerchantId());
        m.put("customerId", tx.getCustomerId());
        m.put("reference", tx.getReference());
        m.put("amountCents", tx.getAmountCents());
        m.put("currency", tx.getCurrency());
        m.put("status", tx.getStatus());
        m.put("paymentMethodType", tx.getPaymentMethodType());
        m.put("locationCity", tx.getLocationCity());
        m.put("locationCountry", tx.getLocationCountry());
        m.put("deviceInfo", tx.getDeviceInfo());
        m.put("ipAddress", tx.getIpAddress());
        m.put("newDevice", tx.isNewDevice());
        m.put("riskScore", tx.getRiskScore());
        m.put("riskLevel", tx.getRiskLevel());
        m.put("createdAt", tx.getCreatedAt());
        m.put("updatedAt", tx.getUpdatedAt());
        return m;
    }

    private Map<String, Object> toRisk(RiskAssessment ra) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ra.getId());
        m.put("riskScore", ra.getRiskScore());
        m.put("riskLevel", ra.getRiskLevel());
        m.put("decision", ra.getDecision());
        m.put("factors", ra.getFactors());
        m.put("assessedAt", ra.getAssessedAt());
        return m;
    }

    private Map<String, Object> toEvent(TransactionEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("eventType", e.getEventType());
        m.put("fromStatus", e.getFromStatus());
        m.put("toStatus", e.getToStatus());
        m.put("message", e.getMessage());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }
}
