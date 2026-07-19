package com.paymentprocessor.authenticationservice.exception;

import org.springframework.http.HttpStatus;

/** Base class for all domain-level errors that map to a specific HTTP status. */
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
