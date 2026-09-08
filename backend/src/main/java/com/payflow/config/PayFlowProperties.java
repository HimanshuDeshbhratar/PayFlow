package com.payflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "payflow")
public class PayFlowProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();
    private Fraud fraud = new Fraud();
    private Idempotency idempotency = new Idempotency();
    private Demo demo = new Demo();
    private Kafka kafka = new Kafka();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpiryMs = 900_000;
        private long refreshTokenExpiryMs = 604_800_000;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));

        public void setAllowedOrigins(List<String> allowedOrigins) {
            if (allowedOrigins != null && allowedOrigins.size() == 1 && allowedOrigins.getFirst().contains(",")) {
                this.allowedOrigins = java.util.Arrays.stream(allowedOrigins.getFirst().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            } else if (allowedOrigins != null) {
                this.allowedOrigins = allowedOrigins;
            }
        }
    }

    @Data
    public static class RateLimit {
        private int loginPerMinute = 5;
        private int paymentPerMinute = 30;
    }

    @Data
    public static class Fraud {
        private int blockThreshold = 80;
        private int reviewThreshold = 60;
        private long highAmountCents = 200_000;
        private int velocityWindowMinutes = 10;
        private int velocityMaxCount = 5;
    }

    @Data
    public static class Idempotency {
        private long ttlSeconds = 86_400;
    }

    @Data
    public static class Demo {
        private String adminEmail = "admin@payflow.demo";
        private String adminPassword = "PayFlowAdmin!2026";
        private String merchantEmail = "merchant@payflow.demo";
        private String merchantPassword = "PayFlowMerchant!2026";
        private String riskEmail = "risk@payflow.demo";
        private String riskPassword = "PayFlowRisk!2026";
    }

    @Data
    public static class Kafka {
        private Topics topics = new Topics();

        @Data
        public static class Topics {
            private String paymentEvents = "payflow.payment.events";
            private String fraudEvents = "payflow.fraud.events";
            private String notificationEvents = "payflow.notification.events";
            private String settlementEvents = "payflow.settlement.events";
        }
    }
}
