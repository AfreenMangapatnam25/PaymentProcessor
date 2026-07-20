package com.paymentprocessor.authorization.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for domain exceptions that carry an HTTP status and a stable machine-readable code.
 */
@Getter
public abstract class ServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ServiceException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    protected ServiceException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
