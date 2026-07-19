package com.paymentprocessor.disputeservice.exception;

/**
 * Thrown when a dispute (or a nested resource) cannot be located by id.
 */
public class DisputeNotFoundException extends RuntimeException {

    public DisputeNotFoundException(String message) {
        super(message);
    }
}
