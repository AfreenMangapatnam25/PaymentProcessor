package com.paymentprocessor.merchantservice.common.error;

/** Thrown on a uniqueness violation. Maps to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
