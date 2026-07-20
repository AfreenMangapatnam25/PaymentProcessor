package com.paymentprocessor.auditservice.service.exception;

/**
 * Thrown when a before/after payload appears to contain raw PII. Audit records must
 * hold only references and redacted/masked values, so such events are rejected outright.
 */
public class PiiDetectedException extends RuntimeException {
    public PiiDetectedException(String message) {
        super(message);
    }
}
