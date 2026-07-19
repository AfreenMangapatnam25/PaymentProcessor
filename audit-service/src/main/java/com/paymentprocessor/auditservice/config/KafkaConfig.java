package com.paymentprocessor.auditservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Kafka consumer error handling: retry with exponential backoff, then route the
 * poison message to a dead-letter topic so ingestion never blocks and nothing is lost.
 */
@Configuration
@ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler auditErrorHandler(KafkaTemplate<?, ?> template,
                                                 AuditProperties props) {
        String dlt = props.getKafka().getDltTopic();

        // KafkaTemplate implements KafkaOperations, which the recoverer accepts.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template, (record, ex) -> new TopicPartition(dlt, record.partition()));

        ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxElapsedTime(60_000L); // give up after ~1 min, then DLT

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Validation / PII failures are not retryable — send straight to the DLT.
        handler.addNotRetryableExceptions(
                com.paymentprocessor.auditservice.service.exception.PiiDetectedException.class,
                com.paymentprocessor.auditservice.service.exception.InvalidAuditEventException.class);
        return handler;
    }
}
