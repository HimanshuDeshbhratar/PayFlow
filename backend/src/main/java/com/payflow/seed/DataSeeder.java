package com.payflow.seed;

import com.payflow.auth.entity.User;
import com.payflow.auth.repository.UserRepository;
import com.payflow.common.enums.*;
import com.payflow.config.PayFlowProperties;
import com.payflow.customer.entity.Customer;
import com.payflow.customer.repository.CustomerRepository;
import com.payflow.fraud.entity.FraudAlert;
import com.payflow.fraud.repository.FraudAlertRepository;
import com.payflow.ledger.service.LedgerService;
import com.payflow.merchant.entity.Merchant;
import com.payflow.merchant.repository.MerchantRepository;
import com.payflow.notification.entity.Notification;
import com.payflow.notification.repository.NotificationRepository;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.repository.PaymentRepository;
import com.payflow.settlement.entity.Settlement;
import com.payflow.settlement.repository.SettlementRepository;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.entity.TransactionEvent;
import com.payflow.transaction.repository.TransactionEventRepository;
import com.payflow.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final SettlementRepository settlementRepository;
    private final NotificationRepository notificationRepository;
    private final LedgerService ledgerService;
    private final PasswordEncoder passwordEncoder;
    private final PayFlowProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Seed skipped — users already present");
            return;
        }
        log.info("Seeding PayFlow demo data...");
        seed();
        log.info("Seed complete");
    }

    private void seed() {
        Merchant merchant = merchantRepository.save(Merchant.builder()
                .businessName("PayFlow Demo Merchant")
                .legalName("PayFlow Demo Merchant LLC")
                .status(MerchantStatus.ACTIVE)
                .countryCode("US")
                .currency("USD")
                .settlementStatus(MerchantSettlementStatus.CURRENT)
                .build());

        var demo = properties.getDemo();
        User admin = saveUser(demo.getAdminEmail(), demo.getAdminPassword(), "PayFlow Admin", UserRole.ADMIN, null);
        User merchantAdmin = saveUser(demo.getMerchantEmail(), demo.getMerchantPassword(),
                "Demo Merchant Admin", UserRole.MERCHANT_ADMIN, merchant.getId());
        User risk = saveUser(demo.getRiskEmail(), demo.getRiskPassword(),
                "Risk Analyst", UserRole.RISK_ANALYST, null);

        ledgerService.getOrCreateMerchantReceivable(merchant.getId(), "USD");

        List<Customer> customers = new ArrayList<>();
        Random random = new Random(42);
        String[] firstNames = {"Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Avery", "Quinn", "Sam", "Jamie"};
        String[] lastNames = {"Smith", "Johnson", "Lee", "Patel", "Garcia", "Kim", "Brown", "Davis", "Wilson", "Nguyen"};
        String[] cities = {"New York", "London", "Berlin", "Tokyo", "Sydney", "Toronto", "UNKNOWN"};
        String[] countries = {"US", "GB", "DE", "JP", "AU", "CA", "NG"};

        for (int i = 0; i < 50; i++) {
            String fn = firstNames[i % firstNames.length];
            String ln = lastNames[(i * 3) % lastNames.length];
            Customer c = customerRepository.save(Customer.builder()
                    .merchantId(merchant.getId())
                    .email(("customer" + (i + 1) + "@payflow.demo").toLowerCase())
                    .fullName(fn + " " + ln)
                    .phone("+1555000" + String.format("%04d", i))
                    .countryCode(countries[i % countries.length])
                    .status(CustomerStatus.ACTIVE)
                    .blocked(false)
                    .build());
            customers.add(c);
            ledgerService.getOrCreateCustomerPayable(c.getId(), "USD");
        }

        PaymentStatus[] weighted = weightedStatuses();
        int txCount = 750;
        List<FraudAlert> alerts = new ArrayList<>();

        for (int i = 0; i < txCount; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            PaymentStatus status = weighted[random.nextInt(weighted.length)];
            long amount = 500L + random.nextInt(250_000);
            boolean newDevice = random.nextDouble() < 0.15;
            String city = cities[random.nextInt(cities.length)];
            String country = countries[random.nextInt(countries.length)];

            Instant created = Instant.now().minus(random.nextInt(60), ChronoUnit.DAYS)
                    .minus(random.nextInt(86_400), ChronoUnit.SECONDS);

            Payment payment = paymentRepository.save(Payment.builder()
                    .merchantId(merchant.getId())
                    .customerId(customer.getId())
                    .reference("pay_seed_" + String.format("%06d", i))
                    .amountCents(amount)
                    .currency("USD")
                    .status(status)
                    .description("Demo payment #" + (i + 1))
                    .checkoutSessionId("cs_seed_" + i)
                    .createdBy(merchantAdmin.getId())
                    .createdAt(created)
                    .updatedAt(created)
                    .build());

            int riskScore = status == PaymentStatus.BLOCKED ? 80 + random.nextInt(20)
                    : status == PaymentStatus.COMPLETED ? random.nextInt(40)
                    : 30 + random.nextInt(40);
            RiskLevel riskLevel = FraudEngineLevel(riskScore);

            Transaction tx = transactionRepository.save(Transaction.builder()
                    .paymentId(payment.getId())
                    .merchantId(merchant.getId())
                    .customerId(customer.getId())
                    .reference("txn_seed_" + String.format("%06d", i))
                    .amountCents(amount)
                    .currency("USD")
                    .status(status)
                    .paymentMethodType(PaymentMethodType.CARD)
                    .locationCity(city)
                    .locationCountry(country)
                    .deviceInfo("Chrome/Windows")
                    .ipAddress("203.0.113." + (i % 250))
                    .newDevice(newDevice)
                    .riskScore(riskScore)
                    .riskLevel(riskLevel)
                    .createdAt(created)
                    .updatedAt(created)
                    .build());

            transactionEventRepository.save(TransactionEvent.builder()
                    .transactionId(tx.getId())
                    .eventType("CREATED")
                    .toStatus(PaymentStatus.PENDING.name())
                    .message("Seeded transaction")
                    .createdAt(created)
                    .build());
            if (status != PaymentStatus.PENDING) {
                transactionEventRepository.save(TransactionEvent.builder()
                        .transactionId(tx.getId())
                        .eventType("STATUS_CHANGE")
                        .fromStatus(PaymentStatus.PENDING.name())
                        .toStatus(status.name())
                        .message("Seeded final status")
                        .createdAt(created.plusSeconds(5))
                        .build());
            }

            if (status == PaymentStatus.COMPLETED) {
                try {
                    ledgerService.postPaymentCompleted(payment, tx);
                } catch (Exception ignored) {
                    // balance already adjusted in bulk seed edge cases
                }
            }

            if (status == PaymentStatus.BLOCKED || riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL) {
                if (alerts.size() < 40) {
                    alerts.add(FraudAlert.builder()
                            .transactionId(tx.getId())
                            .merchantId(merchant.getId())
                            .severity(riskLevel)
                            .title("Suspicious transaction " + tx.getReference())
                            .description("Seeded fraud alert for demo")
                            .status(i % 5 == 0 ? FraudAlertStatus.RESOLVED : FraudAlertStatus.OPEN)
                            .createdAt(created)
                            .build());
                }
            }
        }

        fraudAlertRepository.saveAll(alerts);

        settlementRepository.save(Settlement.builder()
                .merchantId(merchant.getId())
                .reference("stl_seed_001")
                .grossAmountCents(1_250_000)
                .feeAmountCents(36_250)
                .netAmountCents(1_213_750)
                .currency("USD")
                .transactionCount(120)
                .status(SettlementStatus.COMPLETED)
                .settlementDate(LocalDate.now().minusDays(3))
                .processedAt(Instant.now().minus(3, ChronoUnit.DAYS))
                .build());

        for (User u : List.of(admin, merchantAdmin, risk)) {
            notificationRepository.save(Notification.builder()
                    .userId(u.getId())
                    .type("WELCOME")
                    .title("Welcome to PayFlow")
                    .message("Your demo account is ready.")
                    .readFlag(false)
                    .build());
        }

        log.info("Seeded merchant={}, customers={}, payments~={}, alerts={}, users=3",
                merchant.getBusinessName(), customers.size(), txCount, alerts.size());
    }

    private User saveUser(String email, String password, String name, UserRole role, UUID merchantId) {
        return userRepository.save(User.builder()
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .fullName(name)
                .role(role)
                .merchantId(merchantId)
                .active(true)
                .build());
    }

    private PaymentStatus[] weightedStatuses() {
        List<PaymentStatus> list = new ArrayList<>();
        for (int i = 0; i < 70; i++) list.add(PaymentStatus.COMPLETED);
        for (int i = 0; i < 8; i++) list.add(PaymentStatus.BLOCKED);
        for (int i = 0; i < 7; i++) list.add(PaymentStatus.PENDING);
        for (int i = 0; i < 5; i++) list.add(PaymentStatus.FAILED);
        for (int i = 0; i < 4; i++) list.add(PaymentStatus.PROCESSING);
        for (int i = 0; i < 3; i++) list.add(PaymentStatus.PARTIALLY_REFUNDED);
        for (int i = 0; i < 3; i++) list.add(PaymentStatus.REFUNDED);
        return list.toArray(PaymentStatus[]::new);
    }

    private RiskLevel FraudEngineLevel(int score) {
        return com.payflow.fraud.service.FraudEngineService.toLevel(score);
    }
}
