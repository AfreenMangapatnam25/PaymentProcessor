package com.paymentprocessor.disputeservice.event;

import java.time.Instant;

/**
 * Marker interface for dispute domain events published for downstream consumers
 * (Reporting, Analytics, Merchant Portal, Notification, Audit).
 */
public interface DisputeDomainEvent {

    String disputeId();

    String merchantId();

    Instant occurredAt();
}
