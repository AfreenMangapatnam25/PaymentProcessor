package com.paymentprocessor.settlementservice.enums;

/** Delivery state of a transactional-outbox domain event. */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
