package com.paymentprocessor.ledgerservice.web.error;

import org.springframework.http.HttpStatus;

/** A syntactically valid request that violates a business rule (400). */
public class InvalidRequestException extends LedgerException {

    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", HttpStatus.BAD_REQUEST, message);
    }
}
