package com.paymentprocessor.limit.dto;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope returned by the API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<LimitViolationDto> violations
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }
}
