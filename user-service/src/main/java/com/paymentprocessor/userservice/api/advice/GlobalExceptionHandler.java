package com.paymentprocessor.userservice.api.advice;

import com.paymentprocessor.userservice.api.response.ApiResponse;
import com.paymentprocessor.userservice.api.response.Meta;
import com.paymentprocessor.userservice.common.exception.ApplicationException;
import com.paymentprocessor.userservice.common.exception.ConflictException;
import com.paymentprocessor.userservice.common.exception.InfrastructureException;
import com.paymentprocessor.userservice.common.exception.ResourceNotFoundException;
import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.common.model.RequestContext;
import com.paymentprocessor.userservice.common.model.RequestContextFilter;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates exceptions into the standard {@link ApiResponse} envelope. Client
 * errors return their specific status and (safe) message; server errors are
 * logged and returned as a generic 500 so internals and PII never leak
 * (rules 12-13). Every response carries the request/correlation ids for tracing.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final RequestContextFilter contextFilter;

    public GlobalExceptionHandler(RequestContextFilter contextFilter) {
        this.contextFilter = contextFilter;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(ValidationException ex) {
        RequestContext context = contextFilter.getCurrentContext();
        Meta meta = Meta.error(context, HttpStatus.BAD_REQUEST, "Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(ex.getValidationErrors(), meta));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        RequestContext context = contextFilter.getCurrentContext();
        Meta meta = Meta.error(context, HttpStatus.BAD_REQUEST, "Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(fieldErrors, meta));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, "Validation failed");
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ApiResponse<Void>> handleInfrastructure(InfrastructureException ex) {
        // Log the cause server-side; never expose infrastructure detail.
        log.error("Infrastructure failure [{}]", ex.getErrorCode(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Service temporarily unavailable");
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplication(ApplicationException ex) {
        HttpStatus status = ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        if (status.is5xxServerError()) {
            log.error("Application error [{}]", ex.getErrorCode(), ex);
            return error(status, "Internal server error");
        }
        return error(status, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        RequestContext context = contextFilter.getCurrentContext();
        Meta meta = Meta.error(context, status, message);
        return ResponseEntity.status(status).body(ApiResponse.error(meta));
    }
}
