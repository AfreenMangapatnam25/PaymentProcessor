package com.paymentprocessor.authorization.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Translates exceptions into consistent {@link ErrorResponse} payloads with correct HTTP status.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Service error [{}] at {}: {}", ex.getCode(), request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("Service error [{}] at {}: {}", ex.getCode(), request.getRequestURI(), ex.getMessage());
        }
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", request, violations);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request, null);
    }

    private ErrorResponse.FieldViolation toViolation(FieldError error) {
        return ErrorResponse.FieldViolation.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .build();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest request,
                                                List<ErrorResponse.FieldViolation> violations) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .violations(violations)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
