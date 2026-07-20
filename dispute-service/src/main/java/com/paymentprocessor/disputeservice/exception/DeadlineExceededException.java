package com.paymentprocessor.disputeservice.exception;

/**
 * Thrown when an action (e.g. submitting a representment) is attempted after the
 * network response deadline has already passed.
 */
public class DeadlineExceededException extends RuntimeException {

    public DeadlineExceededException(String message) {
        super(message);
    }
}
