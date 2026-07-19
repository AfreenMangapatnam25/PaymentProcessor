package com.paymentprocessor.settlementservice.exception;

/** Thrown when an idempotency key is reused with a different request payload. */
public class IdempotencyConflictException extends SettlementException {

    public IdempotencyConflictException(String key) {
        super("IDEMPOTENCY_CONFLICT",
                "Idempotency-Key '" + key + "' was already used with a different request payload");
    }
}
