package com.paymentprocessor.disputeservice.exception;

/**
 * Thrown when uploaded evidence fails validation (unsupported format, exceeds
 * the size limit, or an incomplete package for the reason code).
 */
public class EvidenceValidationException extends RuntimeException {

    public EvidenceValidationException(String message) {
        super(message);
    }
}
