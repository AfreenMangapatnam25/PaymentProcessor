package com.paymentprocessor.analytics.exception;

/** Raised when an ad-hoc query spec references unknown datasets/columns or invalid ops. */
public class QueryValidationException extends RuntimeException {
    public QueryValidationException(String message) { super(message); }
}
