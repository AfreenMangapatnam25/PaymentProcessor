package com.paymentprocessor.authorization.exception;

import org.springframework.http.HttpStatus;

/** Raised when an operation is not valid for the current authorization state. */
public class InvalidStateException extends ServiceException {
    public InvalidStateException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_STATE", message);
    }
}
