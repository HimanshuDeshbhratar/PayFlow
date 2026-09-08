package com.payflow.refund.service;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.enums.RefundStatus;
import com.payflow.common.exception.PayFlowException;
import com.payflow.event.EventPublisher;
import com.payflow.ledger.service.LedgerService;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.repository.PaymentRepository;
import com.payflow.payment.statemachine.PaymentStateMachine;
import com.payflow.refund.dto.CreateRefundRequest;
import com.payflow.refund.dto.RefundResponse;
import com.payflow.refund.entity.Refund;
import com.payflow.refund.repository.RefundRepository;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.entity.TransactionEvent;
import com.payflow.transaction.repository.TransactionEventRepository;
import com.payflow.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final LedgerService ledgerService;
    private final EventPublisher eventPublisher;
    private final PaymentStateMachine stateMachine;

    @Transactional
    public RefundResponse createRefund(Payment payment, CreateRefundRequest request, String idempotencyKey, UUID requestedBy) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = refundRepository.findByPaymentIdAndIdempotencyKey(payment.getId(), idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        PaymentStatus status = payment.getStatus();
        if (status == PaymentStatus.BLOCKED || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELLED || status == PaymentStatus.PENDING
                || status == PaymentStatus.FRAUD_SCREENING || status == PaymentStatus.PROCESSING) {
            throw PayFlowException.badRequest("Cannot refund payment in status " + status);
        }
        if (status != PaymentStatus.COMPLETED && status != PaymentStatus.PARTIALLY_REFUNDED
                && status != PaymentStatus.REFUNDED) {
            throw PayFlowException.badRequest("Payment is not refundable");
        }
        if (status == PaymentStatus.REFUNDED) {
            throw PayFlowException.badRequest("Payment already fully refunded");
        }

        long alreadyRefunded = refundRepository.sumRefundedExcludingFailed(payment.getId(), RefundStatus.FAILED);
        long remaining = payment.getAmountCents() - alreadyRefunded;
        if (request.amountCents() > remaining) {
            throw PayFlowException.badRequest("Refund amount exceeds remaining refundable amount");
        }

        Transaction tx = transactionRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> PayFlowException.notFound("Transaction not found"));

        Refund refund = Refund.builder()
                .paymentId(payment.getId())
                .transactionId(tx.getId())
                .merchantId(payment.getMerchantId())
                .amountCents(request.amountCents())
                .currency(payment.getCurrency())
                .status(RefundStatus.REQUESTED)
                .reason(request.reason())
                .idempotencyKey(idempotencyKey)
                .requestedBy(requestedBy)
                .build();
        refund = refundRepository.save(refund);

        PaymentStatus fromStatus = payment.getStatus();
        PaymentStatus targetPending = PaymentStatus.REFUND_PENDING;
        stateMachine.assertTransition(fromStatus, targetPending);
        payment.setStatus(targetPending);
        tx.setStatus(targetPending);
        paymentRepository.save(payment);
        transactionRepository.save(tx);
        addEvent(tx, fromStatus, targetPending, "Refund requested");

        refund.setStatus(RefundStatus.PROCESSING);
        refundRepository.save(refund);

        try {
            ledgerService.postRefund(payment, tx, request.amountCents());
            refund.setStatus(RefundStatus.COMPLETED);
            refundRepository.save(refund);

            long totalRefunded = alreadyRefunded + request.amountCents();
            PaymentStatus finalStatus = totalRefunded >= payment.getAmountCents()
                    ? PaymentStatus.REFUNDED
                    : PaymentStatus.PARTIALLY_REFUNDED;
            stateMachine.assertTransition(PaymentStatus.REFUND_PENDING, finalStatus);
            payment.setStatus(finalStatus);
            tx.setStatus(finalStatus);
            paymentRepository.save(payment);
            transactionRepository.save(tx);
            addEvent(tx, PaymentStatus.REFUND_PENDING, finalStatus, "Refund completed");

            eventPublisher.publishPaymentRefunded(payment.getId(), payment.getMerchantId(),
                    EventPublisher.payload("refundId", refund.getId(), "amountCents", request.amountCents()));
        } catch (Exception ex) {
            refund.setStatus(RefundStatus.FAILED);
            refundRepository.save(refund);
            throw PayFlowException.badRequest("Refund failed: " + ex.getMessage());
        }

        return toResponse(refund);
    }

    private void addEvent(Transaction tx, PaymentStatus from, PaymentStatus to, String message) {
        transactionEventRepository.save(TransactionEvent.builder()
                .transactionId(tx.getId())
                .eventType("REFUND")
                .fromStatus(from != null ? from.name() : null)
                .toStatus(to != null ? to.name() : null)
                .message(message)
                .build());
    }

    public RefundResponse toResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getTransactionId(),
                refund.getMerchantId(),
                refund.getAmountCents(),
                refund.getCurrency(),
                refund.getStatus(),
                refund.getReason(),
                refund.getCreatedAt()
        );
    }
}
