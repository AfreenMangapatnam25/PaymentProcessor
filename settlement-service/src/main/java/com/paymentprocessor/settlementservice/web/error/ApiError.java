package com.paymentprocessor.settlementservice.web.error;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body returned by the API.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     HTTP reason phrase
 * @param code      machine-readable domain error code
 * @param message   human-readable message
 * @param path      request path
 * @param details   optional field-level validation errors
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldError> details
) {
    public record FieldError(String field, String message) {
    }
}
