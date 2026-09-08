package com.payflow.payment.statemachine;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.exception.PayFlowException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateMachine {

    private final Map<PaymentStatus, Set<PaymentStatus>> transitions = new EnumMap<>(PaymentStatus.class);

    public PaymentStateMachine() {
        transitions.put(PaymentStatus.PENDING, EnumSet.of(
                PaymentStatus.FRAUD_SCREENING, PaymentStatus.CANCELLED, PaymentStatus.FAILED));
        transitions.put(PaymentStatus.FRAUD_SCREENING, EnumSet.of(
                PaymentStatus.APPROVED, PaymentStatus.BLOCKED, PaymentStatus.FAILED));
        transitions.put(PaymentStatus.APPROVED, EnumSet.of(
                PaymentStatus.PROCESSING, PaymentStatus.FAILED, PaymentStatus.CANCELLED));
        transitions.put(PaymentStatus.BLOCKED, EnumSet.of(PaymentStatus.CANCELLED));
        transitions.put(PaymentStatus.PROCESSING, EnumSet.of(
                PaymentStatus.COMPLETED, PaymentStatus.FAILED));
        transitions.put(PaymentStatus.COMPLETED, EnumSet.of(
                PaymentStatus.REFUND_PENDING, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED));
        transitions.put(PaymentStatus.REFUND_PENDING, EnumSet.of(
                PaymentStatus.REFUNDED, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.FAILED));
        transitions.put(PaymentStatus.PARTIALLY_REFUNDED, EnumSet.of(
                PaymentStatus.REFUND_PENDING, PaymentStatus.REFUNDED));
        transitions.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
        transitions.put(PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class));
        transitions.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
    }

    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        Set<PaymentStatus> allowed = transitions.getOrDefault(from, EnumSet.noneOf(PaymentStatus.class));
        return allowed.contains(to);
    }

    public void assertTransition(PaymentStatus from, PaymentStatus to) {
        if (!canTransition(from, to)) {
            throw PayFlowException.badRequest("Invalid payment transition: " + from + " -> " + to);
        }
    }
}
