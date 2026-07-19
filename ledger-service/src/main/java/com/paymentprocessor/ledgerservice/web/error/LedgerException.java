package com.paymentprocessor.ledgerservice.web.error;

import org.springframework.http.HttpStatus;

/**
 * Base type for all domain-level failures. Carries a machine-readable
 * {@code code} and the HTTP status the API should return.
 */
public class LedgerException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public LedgerException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
