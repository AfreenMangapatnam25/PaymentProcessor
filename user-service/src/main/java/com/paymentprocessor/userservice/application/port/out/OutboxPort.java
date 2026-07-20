package com.paymentprocessor.userservice.application.port.out;

import com.paymentprocessor.userservice.domain.event.DomainEvent;

/**
 * Outbound port for emitting domain events through the transactional outbox.
 * The implementation writes an outbox row in the CURRENT transaction, so the
 * event is committed atomically with the state change that produced it
 * (rule 11). Actual delivery to Kafka happens asynchronously via the relay.
 */
public interface OutboxPort {

    void append(DomainEvent event);
}
