package com.paymentprocessor.ledgerservice.web.error;

import org.springframework.http.HttpStatus;

/**
 * A well-formed request that cannot be processed for a domain reason
 * (422): currency mismatch, inactive account, insufficient funds, closed
 * period, approval required, and similar.
 */
public class UnprocessableException extends LedgerException {

    public UnprocessableException(String code, String message) {
        super(code, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
