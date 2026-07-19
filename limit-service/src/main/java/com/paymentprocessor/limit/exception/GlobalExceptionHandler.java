package com.paymentprocessor.limit.exception;

import com.paymentprocessor.limit.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * A hard limit was exceeded. 422 UNPROCESSABLE_ENTITY signals a business
     * decline (as opposed to a 400 malformed request), carrying the violations.
     */
    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<ApiError> handleLimitExceeded(LimitExceededException ex, HttpServletRequest req) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "LIMIT_EXCEEDED", ex.getMessage(), req.getRequestURI(), ex.getViolations());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(InvalidReservationStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidReservationStateException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(HttpStatus.CONFLICT.value(), "INVALID_STATE", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(HttpStatus.CONFLICT.value(),
                "CONCURRENT_MODIFICATION", "Concurrent update, please retry", req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(
                ApiError.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", message, req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "An unexpected error occurred", req.getRequestURI()));
    }
}
