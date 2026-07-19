package com.paymentprocessor.authenticationservice.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        Instant timestamp,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String error, String message, String path) {
        return new ErrorResponse(error, message, Instant.now(), path, null);
    }
}
