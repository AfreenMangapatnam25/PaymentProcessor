package com.paymentprocessor.authorization.exception;

import org.springframework.http.HttpStatus;

/** Raised when an idempotency key is reused with a conflicting payload. */
public class DuplicateRequestException extends ServiceException {
    public DuplicateRequestException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_REQUEST", message);
    }
}
