package com.paymentprocessor.reconciliationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declares the domain-event topics the service owns (auto-created on brokers that allow it). */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public NewTopic reconciliationCompletedTopic(ReconProperties properties) {
        return TopicBuilder.name(properties.getEvents().getTopic().getReconciliationCompleted())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic mismatchDetectedTopic(ReconProperties properties) {
        return TopicBuilder.name(properties.getEvents().getTopic().getMismatchDetected())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
