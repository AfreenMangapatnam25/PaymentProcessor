package com.paymentprocessor.auditservice.service.exception;

/** Thrown when hash-chain verification detects tampering or a broken link. */
public class ChainIntegrityException extends RuntimeException {
    public ChainIntegrityException(String message) {
        super(message);
    }

    public ChainIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
