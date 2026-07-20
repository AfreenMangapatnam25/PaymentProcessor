package com.paymentprocessor.settlementservice.exception;

import com.paymentprocessor.settlementservice.enums.FailureCategory;

/**
 * Raised when a payment rail rejects or fails a transfer. Carries a rail
 * failure code and its mapped {@link FailureCategory} so the retry engine can
 * decide how to react.
 */
public class RailException extends SettlementException {

    private final String failureCode;
    private final FailureCategory category;

    public RailException(String failureCode, FailureCategory category, String message) {
        super("RAIL_ERROR", message);
        this.failureCode = failureCode;
        this.category = category;
    }

    public String getFailureCode() { return failureCode; }
    public FailureCategory getCategory() { return category; }
}
