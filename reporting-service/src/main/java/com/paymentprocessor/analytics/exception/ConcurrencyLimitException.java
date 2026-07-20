package com.paymentprocessor.analytics.exception;

public class ConcurrencyLimitException extends RuntimeException {
    public ConcurrencyLimitException(String message) { super(message); }
}
