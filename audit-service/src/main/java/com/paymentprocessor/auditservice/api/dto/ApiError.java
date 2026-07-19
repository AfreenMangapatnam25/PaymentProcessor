package com.paymentprocessor.auditservice.api.dto;

import java.time.Instant;

/** Standard error envelope returned by the API. */
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
