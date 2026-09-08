package com.payflow.payment;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.exception.PayFlowException;
import com.payflow.payment.statemachine.PaymentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    PaymentStateMachine sm;

    @BeforeEach
    void setUp() {
        sm = new PaymentStateMachine();
    }

    @Test
    void allowsHappyPath() {
        assertThat(sm.canTransition(PaymentStatus.PENDING, PaymentStatus.FRAUD_SCREENING)).isTrue();
        assertThat(sm.canTransition(PaymentStatus.FRAUD_SCREENING, PaymentStatus.APPROVED)).isTrue();
        assertThat(sm.canTransition(PaymentStatus.APPROVED, PaymentStatus.PROCESSING)).isTrue();
        assertThat(sm.canTransition(PaymentStatus.PROCESSING, PaymentStatus.COMPLETED)).isTrue();
    }

    @Test
    void allowsBlockPath() {
        assertThat(sm.canTransition(PaymentStatus.FRAUD_SCREENING, PaymentStatus.BLOCKED)).isTrue();
        assertThat(sm.canTransition(PaymentStatus.BLOCKED, PaymentStatus.COMPLETED)).isFalse();
    }

    @Test
    void rejectsInvalidJump() {
        assertThatThrownBy(() -> sm.assertTransition(PaymentStatus.PENDING, PaymentStatus.COMPLETED))
                .isInstanceOf(PayFlowException.class);
    }
}
