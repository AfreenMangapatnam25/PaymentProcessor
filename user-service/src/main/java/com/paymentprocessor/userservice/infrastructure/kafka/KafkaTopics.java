package com.paymentprocessor.userservice.infrastructure.kafka;

/**
 * Central registry of the Kafka topics this service publishes to, and the
 * mapping from aggregate type to topic. Keeping this in one place avoids
 * stringly-typed topic names scattered across the code.
 */
public final class KafkaTopics {

    public static final String USERS = "user-service.users.v1";
    public static final String CUSTOMERS = "user-service.customers.v1";
    public static final String ADDRESSES = "user-service.addresses.v1";
    public static final String CONSENTS = "user-service.consents.v1";

    private KafkaTopics() {
    }

    public static String forAggregate(String aggregateType) {
        return switch (aggregateType) {
            case "User" -> USERS;
            case "Customer" -> CUSTOMERS;
            case "Address" -> ADDRESSES;
            case "Consent" -> CONSENTS;
            default -> throw new IllegalArgumentException("No topic mapped for aggregate: " + aggregateType);
        };
    }
}
