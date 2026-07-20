package com.paymentprocessor.merchantservice.common.error;

/** Thrown when a domain rule is violated. Maps to HTTP 422. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
