package com.payflow.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.auth.security.UserPrincipal;
import com.payflow.common.dto.PageResponse;
import com.payflow.common.enums.PaymentMethodType;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.enums.RiskDecision;
import com.payflow.common.enums.UserRole;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import com.payflow.customer.entity.Customer;
import com.payflow.customer.repository.CustomerRepository;
import com.payflow.event.EventPublisher;
import com.payflow.fraud.service.FraudEngineService;
import com.payflow.ledger.service.LedgerService;
import com.payflow.merchant.service.MerchantService;
import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.repository.PaymentRepository;
import com.payflow.payment.statemachine.PaymentStateMachine;
import com.payflow.redis.IdempotencyService;
import com.payflow.refund.dto.CreateRefundRequest;
import com.payflow.refund.dto.RefundResponse;
import com.payflow.refund.service.RefundService;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.entity.TransactionEvent;
import com.payflow.transaction.repository.TransactionEventRepository;
import com.payflow.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final CustomerRepository customerRepository;
    private final MerchantService merchantService;
    private final PaymentStateMachine stateMachine;
    private final FraudEngineService fraudEngineService;
    private final LedgerService ledgerService;
    private final EventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final RefundService refundService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        UserPrincipal user = SecurityUtils.currentUser();
        merchantService.assertCanAccess(request.merchantId());
        merchantService.requireMerchant(request.merchantId());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByMerchantIdAndIdempotencyKey(request.merchantId(), idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
            var cached = idempotencyService.getCachedResponse(request.merchantId(), idempotencyKey);
            if (cached.isPresent()) {
                try {
                    return objectMapper.readValue(cached.get(), PaymentResponse.class);
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }

        if (request.customerId() != null) {
            Customer customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> PayFlowException.notFound("Customer not found"));
            if (!customer.getMerchantId().equals(request.merchantId())) {
                throw PayFlowException.badRequest("Customer does not belong to merchant");
            }
            if (customer.isBlocked()) {
                throw PayFlowException.badRequest("Customer is blocked");
            }
        }

        String reference = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String checkoutSession = "cs_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        Payment payment = Payment.builder()
                .merchantId(request.merchantId())
                .customerId(request.customerId())
                .reference(reference)
                .amountCents(request.amountCents())
                .currency(request.currency() != null ? request.currency() : "USD")
                .status(PaymentStatus.PENDING)
                .description(request.description())
                .idempotencyKey(idempotencyKey)
                .checkoutSessionId(checkoutSession)
                .metadata(request.metadata())
                .createdBy(user.getId())
                .build();

        try {
            payment = paymentRepository.save(payment);
        } catch (Exception ex) {
            if (idempotencyKey != null) {
                return paymentRepository.findByMerchantIdAndIdempotencyKey(request.merchantId(), idempotencyKey)
                        .map(this::toResponse)
                        .orElseThrow(() -> PayFlowException.conflict("Idempotency conflict"));
            }
            throw ex;
        }

        Transaction tx = Transaction.builder()
                .paymentId(payment.getId())
                .merchantId(payment.getMerchantId())
                .customerId(payment.getCustomerId())
                .reference("txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .amountCents(payment.getAmountCents())
                .currency(payment.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentMethodType(request.paymentMethodType() != null ? request.paymentMethodType() : PaymentMethodType.CARD)
                .locationCity(request.locationCity())
                .locationCountry(request.locationCountry())
                .deviceInfo(request.deviceInfo())
                .ipAddress(request.ipAddress())
                .newDevice(Boolean.TRUE.equals(request.newDevice()))
                .build();
        tx = transactionRepository.save(tx);
        addEvent(tx, "CREATED", null, PaymentStatus.PENDING, "Payment created");

        eventPublisher.publishPaymentCreated(payment.getId(), payment.getMerchantId(),
                EventPublisher.payload("reference", payment.getReference(), "amountCents", payment.getAmountCents()));

        processPayment(payment, tx);

        PaymentResponse response = toResponse(paymentRepository.findById(payment.getId()).orElse(payment));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                idempotencyService.storeResponse(request.merchantId(), idempotencyKey,
                        objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                log.debug("Failed to cache idempotent response: {}", e.getMessage());
            }
        }
        return response;
    }

    @Transactional
    public void processPayment(Payment payment, Transaction tx) {
        transition(payment, tx, PaymentStatus.FRAUD_SCREENING, "Fraud screening started");

        FraudEngineService.FraudResult result = fraudEngineService.assessAndPersist(tx);

        if (result.decision() == RiskDecision.BLOCK) {
            transition(payment, tx, PaymentStatus.BLOCKED, "Blocked by fraud engine score=" + result.score());
            eventPublisher.publishPaymentBlocked(payment.getId(), payment.getMerchantId(),
                    EventPublisher.payload("riskScore", result.score(), "riskLevel", result.level()));
            return;
        }

        transition(payment, tx, PaymentStatus.APPROVED, "Approved by fraud engine score=" + result.score());
        eventPublisher.publishPaymentApproved(payment.getId(), payment.getMerchantId(),
                EventPublisher.payload("riskScore", result.score()));

        transition(payment, tx, PaymentStatus.PROCESSING, "Processing payment");

        try {
            if (payment.getCustomerId() == null) {
                throw PayFlowException.badRequest("Customer required to complete payment");
            }
            transition(payment, tx, PaymentStatus.COMPLETED, "Payment completed");
            ledgerService.postPaymentCompleted(payment, tx);
            eventPublisher.publishPaymentCompleted(payment.getId(), payment.getMerchantId(),
                    EventPublisher.payload("amountCents", payment.getAmountCents()));
        } catch (Exception ex) {
            log.error("Payment processing failed for {}: {}", payment.getId(), ex.getMessage());
            if (payment.getStatus() == PaymentStatus.PROCESSING) {
                transition(payment, tx, PaymentStatus.FAILED, "Processing failed: " + ex.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(PaymentStatus status, Pageable pageable) {
        UserPrincipal user = SecurityUtils.currentUser();
        Page<Payment> page;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RISK_ANALYST) {
            page = status != null
                    ? paymentRepository.findByStatus(status, pageable)
                    : paymentRepository.findAll(pageable);
        } else {
            UUID merchantId = user.getMerchantId();
            if (merchantId == null) {
                throw PayFlowException.forbidden("No merchant association");
            }
            page = status != null
                    ? paymentRepository.findByMerchantIdAndStatus(merchantId, status, pageable)
                    : paymentRepository.findByMerchantId(merchantId, pageable);
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID id) {
        Payment payment = requireAccessible(id);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancel(UUID id) {
        Payment payment = requireAccessible(id);
        Transaction tx = transactionRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> PayFlowException.notFound("Transaction not found"));
        transition(payment, tx, PaymentStatus.CANCELLED, "Cancelled by user");
        return toResponse(payment);
    }

    @Transactional
    public RefundResponse refund(UUID paymentId, CreateRefundRequest request, String idempotencyKey) {
        Payment payment = requireAccessible(paymentId);
        return refundService.createRefund(payment, request, idempotencyKey, SecurityUtils.currentUser().getId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkoutGet(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PayFlowException.notFound("Payment not found"));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("paymentId", payment.getId());
        view.put("reference", payment.getReference());
        view.put("amountCents", payment.getAmountCents());
        view.put("currency", payment.getCurrency());
        view.put("status", payment.getStatus());
        view.put("description", payment.getDescription());
        view.put("checkoutSessionId", payment.getCheckoutSessionId());
        return view;
    }

    @Transactional
    public PaymentResponse checkoutConfirm(UUID paymentId, Map<String, Object> body) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PayFlowException.notFound("Payment not found"));
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.APPROVED) {
            return toResponse(payment);
        }
        Transaction tx = transactionRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> PayFlowException.notFound("Transaction not found"));
        if (payment.getStatus() == PaymentStatus.PENDING) {
            processPayment(payment, tx);
        }
        return toResponse(paymentRepository.findById(paymentId).orElse(payment));
    }

    private Payment requireAccessible(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> PayFlowException.notFound("Payment not found"));
        merchantService.assertCanAccess(payment.getMerchantId());
        return payment;
    }

    private void transition(Payment payment, Transaction tx, PaymentStatus to, String message) {
        PaymentStatus from = payment.getStatus();
        stateMachine.assertTransition(from, to);
        payment.setStatus(to);
        tx.setStatus(to);
        paymentRepository.save(payment);
        transactionRepository.save(tx);
        addEvent(tx, "STATUS_CHANGE", from, to, message);
    }

    private void addEvent(Transaction tx, String type, PaymentStatus from, PaymentStatus to, String message) {
        transactionEventRepository.save(TransactionEvent.builder()
                .transactionId(tx.getId())
                .eventType(type)
                .fromStatus(from != null ? from.name() : null)
                .toStatus(to != null ? to.name() : null)
                .message(message)
                .build());
    }

    public PaymentResponse toResponse(Payment payment) {
        Transaction tx = transactionRepository.findByPaymentId(payment.getId()).orElse(null);
        return new PaymentResponse(
                payment.getId(),
                payment.getMerchantId(),
                payment.getCustomerId(),
                payment.getReference(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getDescription(),
                payment.getCheckoutSessionId(),
                tx != null ? tx.getRiskScore() : null,
                tx != null ? tx.getRiskLevel() : null,
                tx != null ? tx.getId() : null,
                payment.getMetadata(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
