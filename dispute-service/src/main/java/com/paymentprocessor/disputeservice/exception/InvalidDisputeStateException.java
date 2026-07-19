package com.paymentprocessor.disputeservice.exception;

/**
 * Thrown when an operation is attempted that is not valid for the dispute's
 * current lifecycle state (e.g. an illegal status transition).
 */
public class InvalidDisputeStateException extends RuntimeException {

    public InvalidDisputeStateException(String message) {
        super(message);
    }
}
