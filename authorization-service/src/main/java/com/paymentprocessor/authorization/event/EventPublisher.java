package com.paymentprocessor.authorization.event;

import com.paymentprocessor.authorization.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Kafka. Publication is asynchronous and failures are logged rather than
 * propagated, so event delivery never fails the originating authorization request. The producer is
 * configured for idempotent, acks=all delivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("eventExecutor")
    public void publishAuthorizationEvent(AuthorizationEvent event) {
        send(KafkaTopicConfig.AUTHORIZATION_EVENTS, event.authorizationId().toString(), event, event.eventType());
    }

    @Async("eventExecutor")
    public void publishAccessControlEvent(AccessControlEvent event) {
        String key = event.identityId() != null ? event.identityId() : event.eventId().toString();
        send(KafkaTopicConfig.ACCESS_CONTROL_EVENTS, key, event, event.eventType());
    }

    private void send(String topic, String key, Object payload, String type) {
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} to {}: {}", type, topic, ex.getMessage());
                } else {
                    log.debug("Published {} to {} (key={})", type, topic, key);
                }
            });
        } catch (Exception ex) {
            log.error("Error publishing {} to {}: {}", type, topic, ex.getMessage());
        }
    }
}
