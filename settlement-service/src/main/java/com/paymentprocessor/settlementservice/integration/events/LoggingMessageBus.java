package com.paymentprocessor.settlementservice.integration.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link MessageBus} that logs published events. Replace with a Kafka /
 * SNS / RabbitMQ producer in production.
 */
@Component
public class LoggingMessageBus implements MessageBus {

    private static final Logger log = LoggerFactory.getLogger("settlement.events");

    @Override
    public void send(String eventType, String aggregateType, String aggregateId, String payloadJson) {
        log.info("EVENT {} [{}#{}] {}", eventType, aggregateType, aggregateId, payloadJson);
    }
}
