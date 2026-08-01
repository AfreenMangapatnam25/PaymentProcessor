package com.paymentprocessor.userservice.infrastructure.kafka;

/**
 * Central registry of the Kafka topic this service publishes to.
 * All aggregate types share {@link #SERVICE_TOPIC} per platform convention.
 */
public final class KafkaTopics {

    public static final String SERVICE_TOPIC = "userservicetopic";

    private KafkaTopics() {
    }

    public static String forAggregate(String aggregateType) {
        return switch (aggregateType) {
            case "User", "Customer", "Address", "Consent" -> SERVICE_TOPIC;
            default -> throw new IllegalArgumentException("No topic mapped for aggregate: " + aggregateType);
        };
    }
}
