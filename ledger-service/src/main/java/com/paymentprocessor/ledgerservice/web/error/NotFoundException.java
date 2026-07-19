package com.paymentprocessor.ledgerservice.web.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends LedgerException {

    public NotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }

    public static NotFoundException of(String what, String id) {
        return new NotFoundException(what + " not found: " + id);
    }
}
