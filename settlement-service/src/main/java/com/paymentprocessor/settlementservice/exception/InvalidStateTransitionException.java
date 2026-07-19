package com.paymentprocessor.settlementservice.exception;

/** Thrown when a state machine transition is not permitted. */
public class InvalidStateTransitionException extends SettlementException {

    public InvalidStateTransitionException(String entity, Object from, Object to) {
        super("INVALID_STATE_TRANSITION",
                entity + " cannot transition from " + from + " to " + to);
    }
}
