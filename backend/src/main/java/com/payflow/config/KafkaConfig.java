package com.payflow.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final PayFlowProperties properties;

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(properties.getKafka().getTopics().getPaymentEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudEventsTopic() {
        return TopicBuilder.name(properties.getKafka().getTopics().getFraudEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(properties.getKafka().getTopics().getNotificationEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic settlementEventsTopic() {
        return TopicBuilder.name(properties.getKafka().getTopics().getSettlementEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
