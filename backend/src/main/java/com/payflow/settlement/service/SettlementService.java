package com.payflow.settlement.service;

import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.enums.SettlementStatus;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.event.EventPublisher;
import com.payflow.merchant.service.MerchantService;
import com.payflow.settlement.entity.Settlement;
import com.payflow.settlement.repository.SettlementRepository;
import com.payflow.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final TransactionRepository transactionRepository;
    private final MerchantService merchantService;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(UUID merchantId, Pageable pageable) {
        var user = SecurityUtils.currentUser();
        if (user.getRole() == UserRole.ADMIN) {
            if (merchantId != null) {
                return PageResponse.from(settlementRepository.findByMerchantId(merchantId, pageable).map(this::toMap));
            }
            return PageResponse.from(settlementRepository.findAll(pageable).map(this::toMap));
        }
        UUID mid = user.getMerchantId();
        if (mid == null) {
            throw PayFlowException.forbidden("No merchant association");
        }
        return PageResponse.from(settlementRepository.findByMerchantId(mid, pageable).map(this::toMap));
    }

    @Transactional
    public Map<String, Object> createForMerchant(UUID merchantId) {
        merchantService.assertCanAccess(merchantId);
        long gross = transactionRepository.sumAmountByMerchantAndStatus(merchantId, PaymentStatus.COMPLETED);
        long fee = Math.round(gross * 0.029);
        long net = gross - fee;
        Settlement settlement = settlementRepository.save(Settlement.builder()
                .merchantId(merchantId)
                .reference("stl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .grossAmountCents(gross)
                .feeAmountCents(fee)
                .netAmountCents(net)
                .currency("USD")
                .transactionCount((int) Math.min(Integer.MAX_VALUE, transactionRepository
                        .findByMerchantIdAndStatus(merchantId, PaymentStatus.COMPLETED, Pageable.unpaged()).getTotalElements()))
                .status(SettlementStatus.COMPLETED)
                .settlementDate(LocalDate.now())
                .processedAt(Instant.now())
                .build());
        eventPublisher.publishSettlementCompleted(settlement.getId(), merchantId,
                EventPublisher.payload("netAmountCents", net, "reference", settlement.getReference()));
        return toMap(settlement);
    }

    private Map<String, Object> toMap(Settlement s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("merchantId", s.getMerchantId());
        m.put("reference", s.getReference());
        m.put("grossAmountCents", s.getGrossAmountCents());
        m.put("feeAmountCents", s.getFeeAmountCents());
        m.put("netAmountCents", s.getNetAmountCents());
        m.put("currency", s.getCurrency());
        m.put("transactionCount", s.getTransactionCount());
        m.put("status", s.getStatus());
        m.put("settlementDate", s.getSettlementDate());
        m.put("processedAt", s.getProcessedAt());
        m.put("createdAt", s.getCreatedAt());
        return m;
    }
}
