package com.paymentprocessor.settlementservice.web.error;

import com.paymentprocessor.settlementservice.exception.ApprovalRequiredException;
import com.paymentprocessor.settlementservice.exception.BadRequestException;
import com.paymentprocessor.settlementservice.exception.IdempotencyConflictException;
import com.paymentprocessor.settlementservice.exception.InvalidStateTransitionException;
import com.paymentprocessor.settlementservice.exception.RailException;
import com.paymentprocessor.settlementservice.exception.ResourceNotFoundException;
import com.paymentprocessor.settlementservice.exception.SettlementException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain and framework exceptions into consistent {@link ApiError}
 * responses with appropriate HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler({BadRequestException.class, InvalidStateTransitionException.class})
    public ResponseEntity<ApiError> handleBadRequest(SettlementException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(ApprovalRequiredException.class)
    public ResponseEntity<ApiError> handleApproval(ApprovalRequiredException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotency(IdempotencyConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was modified concurrently; please retry", req, null);
    }

    @ExceptionHandler(RailException.class)
    public ResponseEntity<ApiError> handleRail(RailException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_GATEWAY, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", req, details);
    }

    @ExceptionHandler(SettlementException.class)
    public ResponseEntity<ApiError> handleDomain(SettlementException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", req, null);
    }

    private ApiError.FieldError toFieldError(FieldError fe) {
        return new ApiError.FieldError(fe.getField(), fe.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest req, List<ApiError.FieldError> details) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                code, message, req.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
