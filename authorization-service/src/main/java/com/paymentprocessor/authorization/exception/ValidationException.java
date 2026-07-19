package com.paymentprocessor.authorization.exception;

import org.springframework.http.HttpStatus;

/** Raised for business-rule validation failures not expressible as bean-validation constraints. */
public class ValidationException extends ServiceException {
    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
