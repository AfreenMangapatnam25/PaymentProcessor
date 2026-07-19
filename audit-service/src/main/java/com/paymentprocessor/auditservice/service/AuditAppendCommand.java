package com.paymentprocessor.auditservice.service;

import java.time.Instant;
import java.util.Map;

import com.paymentprocessor.auditservice.domain.Actor;
import com.paymentprocessor.auditservice.domain.ResourceRef;

/**
 * Transport-neutral command to append one audit record. Both the REST controller and
 * the Kafka consumer map their inputs to this, so ingestion logic lives in one place.
 *
 * @param eventId    producer-supplied idempotency key (may be null; strongly recommended)
 * @param ts         business timestamp of the event
 * @param actor      who performed the action
 * @param action     dotted action name, e.g. {@code payout_account.update}
 * @param resource   the affected resource reference
 * @param merchantId owning merchant reference
 * @param before     redacted prior state (references/masked values only)
 * @param after      redacted new state (references/masked values only)
 * @param requestId  originating request id
 * @param traceId    distributed trace id
 */
public record AuditAppendCommand(
        String eventId,
        Instant ts,
        Actor actor,
        String action,
        ResourceRef resource,
        String merchantId,
        Map<String, Object> before,
        Map<String, Object> after,
        String requestId,
        String traceId) {
}
