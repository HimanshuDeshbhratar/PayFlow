package com.payflow.fraud;

import com.payflow.config.PayFlowProperties;
import com.payflow.fraud.entity.FraudRule;
import com.payflow.fraud.repository.FraudAlertRepository;
import com.payflow.fraud.repository.FraudRuleRepository;
import com.payflow.fraud.repository.RiskAssessmentRepository;
import com.payflow.fraud.service.FraudEngineService;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FraudEngineTest {

    @Mock FraudRuleRepository fraudRuleRepository;
    @Mock RiskAssessmentRepository riskAssessmentRepository;
    @Mock FraudAlertRepository fraudAlertRepository;
    @Mock TransactionRepository transactionRepository;

    PayFlowProperties properties;
    FraudEngineService service;

    @BeforeEach
    void setUp() {
        properties = new PayFlowProperties();
        properties.getFraud().setHighAmountCents(200_000);
        properties.getFraud().setBlockThreshold(80);
        properties.getFraud().setReviewThreshold(60);
        properties.getFraud().setVelocityMaxCount(5);
        properties.getFraud().setVelocityWindowMinutes(10);
        service = new FraudEngineService(fraudRuleRepository, riskAssessmentRepository,
                fraudAlertRepository, transactionRepository, properties);

        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(
                rule("HIGH_AMOUNT", 30),
                rule("NEW_DEVICE", 20),
                rule("UNUSUAL_LOCATION", 20),
                rule("VELOCITY", 15),
                rule("FIRST_MERCHANT", 10),
                rule("SUSPICIOUS_IP", 25)
        ));
    }

    @Test
    void lowRiskWhenNoFactors() {
        Transaction tx = baseTx();
        tx.setAmountCents(1000);
        tx.setNewDevice(false);
        when(transactionRepository.countByCustomerAndMerchantSince(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.countByCustomerAndMerchant(any(), any())).thenReturn(5L);

        var result = service.calculate(tx);
        assertThat(result.score()).isLessThan(30);
        assertThat(result.level().name()).isEqualTo("LOW");
        assertThat(result.decision().name()).isEqualTo("APPROVE");
    }

    @Test
    void highAmountAndSuspiciousIpBlocks() {
        Transaction tx = baseTx();
        tx.setAmountCents(500_000);
        tx.setNewDevice(true);
        tx.setIpAddress("10.255.1.1");
        tx.setLocationCountry("NG");
        when(transactionRepository.countByCustomerAndMerchantSince(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.countByCustomerAndMerchant(any(), any())).thenReturn(5L);

        var result = service.calculate(tx);
        // HIGH_AMOUNT 30 + NEW_DEVICE 20 + UNUSUAL_LOCATION 20 + SUSPICIOUS_IP 25 = 95
        assertThat(result.score()).isGreaterThanOrEqualTo(80);
        assertThat(result.decision().name()).isEqualTo("BLOCK");
        assertThat(result.factors()).isNotEmpty();
    }

    @Test
    void riskLevelBands() {
        assertThat(FraudEngineService.toLevel(0).name()).isEqualTo("LOW");
        assertThat(FraudEngineService.toLevel(29).name()).isEqualTo("LOW");
        assertThat(FraudEngineService.toLevel(30).name()).isEqualTo("MEDIUM");
        assertThat(FraudEngineService.toLevel(59).name()).isEqualTo("MEDIUM");
        assertThat(FraudEngineService.toLevel(60).name()).isEqualTo("HIGH");
        assertThat(FraudEngineService.toLevel(79).name()).isEqualTo("HIGH");
        assertThat(FraudEngineService.toLevel(80).name()).isEqualTo("CRITICAL");
    }

    private Transaction baseTx() {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .reference("txn_test")
                .amountCents(1000)
                .currency("USD")
                .locationCity("New York")
                .locationCountry("US")
                .ipAddress("8.8.8.8")
                .newDevice(false)
                .build();
    }

    private FraudRule rule(String code, int weight) {
        return FraudRule.builder().code(code).name(code).weight(weight).enabled(true).build();
    }
}
