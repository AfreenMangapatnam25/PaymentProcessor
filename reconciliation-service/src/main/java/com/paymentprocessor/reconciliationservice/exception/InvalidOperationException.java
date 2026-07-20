package com.paymentprocessor.reconciliationservice.exception;

/** Thrown when an operation is not valid for the current state of a resource. Mapped to HTTP 409. */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
