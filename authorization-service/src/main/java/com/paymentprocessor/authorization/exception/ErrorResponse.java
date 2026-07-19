package com.paymentprocessor.authorization.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * RFC-7807-inspired error payload returned by the {@link GlobalExceptionHandler}.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> violations
) {
    @Builder
    public record FieldViolation(String field, String message) {
    }
}
