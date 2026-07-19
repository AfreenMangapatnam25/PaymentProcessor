package com.paymentprocessor.auditservice.service.exception;

/** Thrown when an incoming audit event fails structural/semantic validation. */
public class InvalidAuditEventException extends RuntimeException {
    public InvalidAuditEventException(String message) {
        super(message);
    }
}
