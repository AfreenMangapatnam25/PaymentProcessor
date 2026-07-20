package com.paymentprocessor.reconciliationservice.exception;

import java.time.Instant;
import java.util.List;

/** Standard error response body returned by the API. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
}
