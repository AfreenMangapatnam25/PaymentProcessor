package com.paymentprocessor.authorization.exception;

import org.springframework.http.HttpStatus;

/** Raised when the caller is not permitted to perform the requested operation. */
public class AccessDeniedException extends ServiceException {
    public AccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "ACCESS_DENIED", message);
    }
}
