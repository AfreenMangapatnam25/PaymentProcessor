package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * A fact that has happened in the domain. Emitted transactionally through the
 * outbox (rule 11). Implementations MUST carry ids and non-PII attributes only
 * (rules 12-13); raw PII never travels on an event.
 */
public interface DomainEvent {

    /** Stable business key of the aggregate that emitted the event. */
    String aggregateId();

    /** Aggregate type, e.g. {@code "User"}. */
    String aggregateType();

    /** Versioned event type, e.g. {@code "user.created.v1"}. */
    String eventType();

    /** When the fact occurred (UTC). */
    Instant occurredAt();
}
