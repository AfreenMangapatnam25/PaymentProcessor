package com.paymentprocessor.limit.event;

import com.paymentprocessor.limit.config.LimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes limit domain events to Kafka. Failures are logged but never propagate
 * to the caller: event publication is best-effort and must not fail a payment
 * authorization decision that has already been persisted.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LimitEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LimitProperties properties;

    public void publishReserved(LimitEvent.LimitReserved event) {
        send(properties.getEvents().getReservedTopic(), event.transactionId(), event);
    }

    public void publishReleased(LimitEvent.LimitReleased event) {
        send(properties.getEvents().getReleasedTopic(), event.transactionId(), event);
    }

    public void publishExceeded(LimitEvent.LimitExceeded event) {
        send(properties.getEvents().getExceededTopic(), event.transactionId(), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
            log.debug("Published event to {} key={}", topic, key);
        } catch (Exception ex) {
            log.error("Failed to publish event to {} key={}: {}", topic, key, ex.getMessage(), ex);
        }
    }
}
