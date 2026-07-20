package com.paymentprocessor.disputeservice.exception;

/**
 * Thrown when a dispute is received for a chargeback reference that has already
 * been recorded.
 */
public class DuplicateDisputeException extends RuntimeException {

    public DuplicateDisputeException(String message) {
        super(message);
    }
}
