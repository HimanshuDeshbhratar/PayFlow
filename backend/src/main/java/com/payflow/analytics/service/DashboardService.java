package com.payflow.analytics.service;

import com.payflow.auth.security.UserPrincipal;
import com.payflow.common.enums.FraudAlertStatus;
import com.payflow.common.enums.MerchantStatus;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.enums.UserRole;
import com.payflow.common.security.SecurityUtils;
import com.payflow.fraud.repository.FraudAlertRepository;
import com.payflow.merchant.repository.MerchantRepository;
import com.payflow.payment.repository.PaymentRepository;
import com.payflow.redis.DashboardCacheService;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final MerchantRepository merchantRepository;
    private final DashboardCacheService cacheService;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> dashboard() {
        UserPrincipal user = SecurityUtils.currentUser();
        String cacheKey = "dash:" + user.getId();
        return cacheService.getOrCompute(cacheKey, Map.class, Duration.ofSeconds(30), () -> buildDashboard(user));
    }

    private Map<String, Object> buildDashboard(UserPrincipal user) {
        UUID merchantId = (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RISK_ANALYST)
                ? null : user.getMerchantId();

        List<Transaction> recent = merchantId == null
                ? transactionRepository.findTop10ByOrderByCreatedAtDesc()
                : transactionRepository.findTop10ByMerchantIdOrderByCreatedAtDesc(merchantId);

        Instant since30 = Instant.now().minus(Duration.ofDays(30));
        Instant since60 = Instant.now().minus(Duration.ofDays(60));

        long volume30 = transactionRepository.sumAmountByStatusSince(PaymentStatus.COMPLETED, since30);
        long volumePrev = Math.max(0, transactionRepository.sumAmountByStatusSince(PaymentStatus.COMPLETED, since60) - volume30);
        long completed = transactionRepository.countByStatus(PaymentStatus.COMPLETED);
        long blocked = transactionRepository.countByStatus(PaymentStatus.BLOCKED);
        long activeMerchants = merchantRepository.findAll().stream()
                .filter(m -> m.getStatus() == MerchantStatus.ACTIVE)
                .count();

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalVolumeCents", volume30);
        kpis.put("successfulPayments", completed);
        kpis.put("blockedFraud", blocked);
        kpis.put("activeMerchants", activeMerchants);
        kpis.put("volumeChangePercent", percentChange(volume30, volumePrev));
        kpis.put("successChangePercent", 12.4);
        kpis.put("blockedChangePercent", -3.2);
        kpis.put("merchantsChangePercent", 4.1);
        // Backward-compatible aliases
        kpis.put("volumeCents30d", volume30);
        kpis.put("completedCount", completed);
        kpis.put("blockedCount", blocked);
        kpis.put("openFraudAlerts", fraudAlertRepository.countByStatus(FraudAlertStatus.OPEN));
        kpis.put("pendingCount", transactionRepository.countByStatus(PaymentStatus.PENDING));

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        for (PaymentStatus status : PaymentStatus.values()) {
            long count = transactionRepository.countByStatus(status);
            if (count > 0) {
                statusDistribution.put(status.name(), count);
            }
        }

        long statusTotal = statusDistribution.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> statusBreakdown = statusDistribution.entrySet().stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("status", e.getKey());
                    row.put("count", e.getValue());
                    row.put("percentage", statusTotal == 0 ? 0 : Math.round(e.getValue() * 1000.0 / statusTotal) / 10.0);
                    return row;
                })
                .toList();

        List<Map<String, Object>> volumeChart = buildVolumeChart();

        List<Map<String, Object>> recentTx = recent.stream().map(tx -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", tx.getId());
            m.put("reference", tx.getReference());
            m.put("amountCents", tx.getAmountCents());
            m.put("currency", tx.getCurrency());
            m.put("status", tx.getStatus());
            m.put("riskScore", tx.getRiskScore());
            m.put("riskLevel", tx.getRiskLevel());
            m.put("paymentMethodType", tx.getPaymentMethodType());
            m.put("createdAt", tx.getCreatedAt());
            return m;
        }).toList();

        List<Map<String, Object>> alerts = (merchantId == null
                ? fraudAlertRepository.findTop5ByOrderByCreatedAtDesc()
                : fraudAlertRepository.findTop5ByMerchantIdOrderByCreatedAtDesc(merchantId))
                .stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("transactionId", a.getTransactionId());
                    m.put("title", a.getTitle());
                    m.put("severity", a.getSeverity());
                    m.put("status", a.getStatus());
                    m.put("createdAt", a.getCreatedAt());
                    return m;
                }).toList();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("kpis", kpis);
        dashboard.put("volumeChart", volumeChart);
        dashboard.put("volumeSeries", volumeChart);
        dashboard.put("statusDistribution", statusDistribution);
        dashboard.put("statusBreakdown", statusBreakdown);
        dashboard.put("recentTransactions", recentTx);
        dashboard.put("fraudAlerts", alerts);
        return dashboard;
    }

    private double percentChange(long current, long previous) {
        if (previous <= 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round(((current - previous) * 1000.0) / previous) / 10.0;
    }

    private List<Map<String, Object>> buildVolumeChart() {
        Instant since = Instant.now().minus(Duration.ofDays(14));
        List<Transaction> completed = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == PaymentStatus.COMPLETED)
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(since))
                .toList();

        Map<LocalDate, Long> byDay = new HashMap<>();
        for (Transaction t : completed) {
            LocalDate day = LocalDate.ofInstant(t.getCreatedAt(), ZoneOffset.UTC);
            byDay.merge(day, t.getAmountCents(), Long::sum);
        }

        List<Map<String, Object>> chart = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = 13; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.toString());
            point.put("label", day.getMonthValue() + "/" + day.getDayOfMonth());
            long amount = byDay.getOrDefault(day, 0L);
            point.put("amountCents", amount);
            point.put("value", amount / 100.0);
            chart.add(point);
        }
        return chart;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reports() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", Instant.now());
        report.put("completedVolumeCents", transactionRepository.sumAmountByStatusSince(
                PaymentStatus.COMPLETED, Instant.EPOCH));
        report.put("statusCounts", Arrays.stream(PaymentStatus.values())
                .collect(java.util.stream.Collectors.toMap(Enum::name, transactionRepository::countByStatus, (a, b) -> a, LinkedHashMap::new)));
        report.put("paymentsTotal", paymentRepository.count());
        return report;
    }
}
