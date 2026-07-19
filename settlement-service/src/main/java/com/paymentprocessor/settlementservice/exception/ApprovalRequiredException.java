package com.paymentprocessor.settlementservice.exception;

/** Thrown when an action requires a higher approval level than was supplied. */
public class ApprovalRequiredException extends SettlementException {

    public ApprovalRequiredException(String message) {
        super("APPROVAL_REQUIRED", message);
    }
}
