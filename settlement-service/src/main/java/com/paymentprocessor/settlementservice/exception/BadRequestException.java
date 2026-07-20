package com.paymentprocessor.settlementservice.exception;

/** Thrown for semantically invalid requests that pass basic bean validation. */
public class BadRequestException extends SettlementException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }
}
