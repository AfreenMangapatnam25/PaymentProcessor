package com.paymentprocessor.fraudservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.paymentprocessor.fraudservice.dto.FraudDecisionResponse;
import com.paymentprocessor.fraudservice.engine.Decision;

/**
 * Translates a decision into the appropriate domain event and publishes it on
 * the application event bus.
 */
@Component
public class FraudEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FraudEventPublisher.class);

    private final ApplicationEventPublisher publisher;

    public FraudEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(FraudDecisionResponse response) {
        FraudEvent.Type type = mapType(Decision.valueOf(response.getDecision()));
        log.info("Publishing {} for intent={} score={} decision={}",
                type, response.getIntentId(), response.getRiskScore(), response.getDecision());
        publisher.publishEvent(FraudEvent.of(type, response));
    }

    private FraudEvent.Type mapType(Decision decision) {
        switch (decision) {
            case APPROVE:   return FraudEvent.Type.FRAUD_APPROVED;
            case CHALLENGE: return FraudEvent.Type.FRAUD_DETECTED;
            case REVIEW:    return FraudEvent.Type.MANUAL_REVIEW_REQUIRED;
            case DECLINE:   return FraudEvent.Type.FRAUD_REJECTED;
            case ESCALATE:  return FraudEvent.Type.FRAUD_ESCALATED;
            default:        return FraudEvent.Type.FRAUD_DETECTED;
        }
    }
}
