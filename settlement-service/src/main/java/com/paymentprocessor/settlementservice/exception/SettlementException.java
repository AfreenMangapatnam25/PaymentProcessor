package com.paymentprocessor.settlementservice.exception;

/** Base type for all domain-level settlement errors. */
public abstract class SettlementException extends RuntimeException {

    private final String code;

    protected SettlementException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected SettlementException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
