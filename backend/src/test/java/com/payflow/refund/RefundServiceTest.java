package com.payflow.refund;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.enums.RefundStatus;
import com.payflow.common.exception.PayFlowException;
import com.payflow.event.EventPublisher;
import com.payflow.ledger.service.LedgerService;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.repository.PaymentRepository;
import com.payflow.payment.statemachine.PaymentStateMachine;
import com.payflow.refund.dto.CreateRefundRequest;
import com.payflow.refund.entity.Refund;
import com.payflow.refund.repository.RefundRepository;
import com.payflow.refund.service.RefundService;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.repository.TransactionEventRepository;
import com.payflow.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock RefundRepository refundRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock TransactionEventRepository transactionEventRepository;
    @Mock LedgerService ledgerService;
    @Mock EventPublisher eventPublisher;

    RefundService service;
    Payment payment;
    Transaction tx;

    @BeforeEach
    void setUp() {
        service = new RefundService(refundRepository, paymentRepository, transactionRepository,
                transactionEventRepository, ledgerService, eventPublisher, new PaymentStateMachine());

        UUID paymentId = UUID.randomUUID();
        payment = Payment.builder()
                .id(paymentId)
                .merchantId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .reference("pay_1")
                .amountCents(10_000)
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .build();
        tx = Transaction.builder()
                .id(UUID.randomUUID())
                .paymentId(paymentId)
                .merchantId(payment.getMerchantId())
                .customerId(payment.getCustomerId())
                .reference("txn_1")
                .amountCents(10_000)
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .build();
    }

    @Test
    void rejectsBlockedPayment() {
        payment.setStatus(PaymentStatus.BLOCKED);
        assertThatThrownBy(() -> service.createRefund(payment, new CreateRefundRequest(1000L, "x"), null, UUID.randomUUID()))
                .isInstanceOf(PayFlowException.class);
    }

    @Test
    void rejectsOverRefund() {
        when(refundRepository.sumRefundedExcludingFailed(payment.getId(), RefundStatus.FAILED)).thenReturn(9_000L);
        assertThatThrownBy(() -> service.createRefund(payment, new CreateRefundRequest(2_000L, "x"), null, UUID.randomUUID()))
                .isInstanceOf(PayFlowException.class);
    }

    @Test
    void completesPartialRefund() {
        when(refundRepository.sumRefundedExcludingFailed(payment.getId(), RefundStatus.FAILED)).thenReturn(0L);
        when(transactionRepository.findByPaymentId(payment.getId())).thenReturn(Optional.of(tx));
        when(refundRepository.save(any())).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createRefund(payment, new CreateRefundRequest(4_000L, "partial"), "idem-1", UUID.randomUUID());

        assertThat(response.status()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        verify(ledgerService).postRefund(payment, tx, 4_000L);
    }

    @Test
    void isIdempotent() {
        Refund existing = Refund.builder()
                .id(UUID.randomUUID())
                .paymentId(payment.getId())
                .transactionId(UUID.randomUUID())
                .merchantId(payment.getMerchantId())
                .amountCents(1000)
                .currency("USD")
                .status(RefundStatus.COMPLETED)
                .idempotencyKey("idem")
                .build();
        when(refundRepository.findByPaymentIdAndIdempotencyKey(payment.getId(), "idem"))
                .thenReturn(Optional.of(existing));

        var response = service.createRefund(payment, new CreateRefundRequest(1000L, "x"), "idem", UUID.randomUUID());
        assertThat(response.id()).isEqualTo(existing.getId());
        verify(ledgerService, never()).postRefund(any(), any(), anyLong());
    }
}
