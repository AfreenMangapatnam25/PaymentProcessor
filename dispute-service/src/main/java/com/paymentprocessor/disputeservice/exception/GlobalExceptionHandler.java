package com.paymentprocessor.disputeservice.exception;

import com.paymentprocessor.disputeservice.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain and validation exceptions into structured HTTP error
 * responses so controllers can stay free of try/catch boilerplate.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DisputeNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(DisputeNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateDisputeException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateDisputeException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler({InvalidDisputeStateException.class, EvidenceValidationException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(DeadlineExceededException.class)
    public ResponseEntity<ApiError> handleDeadline(DeadlineExceededException ex, HttpServletRequest req) {
        // 422: the request was well-formed but cannot be actioned given the deadline.
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message,
                req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
