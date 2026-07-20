package com.paymentprocessor.settlementservice.integration.events;

/**
 * The external event transport (Kafka, SNS, RabbitMQ, ...). The default
 * implementation logs; swap it for a broker client in production.
 */
public interface MessageBus {

    void send(String eventType, String aggregateType, String aggregateId, String payloadJson);
}
