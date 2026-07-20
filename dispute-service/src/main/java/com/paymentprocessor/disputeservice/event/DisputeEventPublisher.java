package com.paymentprocessor.disputeservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over Spring's {@link ApplicationEventPublisher} used by services
 * to emit dispute domain events. In production the listener would forward these
 * onto a durable message bus (Kafka / SNS) for downstream services.
 */
@Component
public class DisputeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DisputeEventPublisher.class);

    private final ApplicationEventPublisher delegate;

    public DisputeEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    public void publish(DisputeDomainEvent event) {
        log.debug("Publishing domain event {} for dispute {}",
                event.getClass().getSimpleName(), event.disputeId());
        delegate.publishEvent(event);
    }
}
