package com.paymentprocessor.ledgerservice.web.error;

import org.springframework.http.HttpStatus;

/** The request conflicts with the current state of a resource (409). */
public class ConflictException extends LedgerException {

    public ConflictException(String code, String message) {
        super(code, HttpStatus.CONFLICT, message);
    }
}
