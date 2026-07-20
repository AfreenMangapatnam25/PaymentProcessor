package com.paymentprocessor.authorization.exception;

import org.springframework.http.HttpStatus;

/** Raised when a protected operation is invoked without a valid identity token. */
public class UnauthenticatedException extends ServiceException {
    public UnauthenticatedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", message);
    }
}
