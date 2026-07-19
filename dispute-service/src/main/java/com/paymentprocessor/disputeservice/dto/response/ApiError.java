package com.paymentprocessor.disputeservice.dto.response;

import java.time.Instant;

/**
 * Structured error body returned by the API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
