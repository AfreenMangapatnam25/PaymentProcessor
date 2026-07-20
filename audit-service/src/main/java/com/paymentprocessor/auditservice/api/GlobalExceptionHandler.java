package com.paymentprocessor.auditservice.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.paymentprocessor.auditservice.api.dto.ApiError;
import com.paymentprocessor.auditservice.service.exception.ChainIntegrityException;
import com.paymentprocessor.auditservice.service.exception.InvalidAuditEventException;
import com.paymentprocessor.auditservice.service.exception.PiiDetectedException;
import com.paymentprocessor.auditservice.service.exception.ResourceNotFoundException;

/** Maps service exceptions to consistent HTTP responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler({PiiDetectedException.class, InvalidAuditEventException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, detail, req);
    }

    @ExceptionHandler(ChainIntegrityException.class)
    public ResponseEntity<ApiError> handleIntegrity(ChainIntegrityException ex, WebRequest req) {
        log.error("Chain integrity error", ex);
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest req) {
        log.error("Unexpected error handling request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest req) {
        String path = req.getDescription(false).replaceFirst("^uri=", "");
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message, path));
    }
}
