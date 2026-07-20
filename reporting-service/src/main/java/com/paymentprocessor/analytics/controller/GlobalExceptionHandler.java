package com.paymentprocessor.analytics.controller;

import com.paymentprocessor.analytics.exception.ConcurrencyLimitException;
import com.paymentprocessor.analytics.exception.QueryValidationException;
import com.paymentprocessor.analytics.exception.ReportNotFoundException;
import com.paymentprocessor.analytics.exception.ReportNotReadyException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to RFC-7807 ProblemDetail responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(QueryValidationException.class)
    public ProblemDetail onValidation(QueryValidationException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onBeanValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ProblemDetail onNotFound(ReportNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(ReportNotReadyException.class)
    public ProblemDetail onNotReady(ReportNotReadyException e) {
        return problem(HttpStatus.CONFLICT, "Report not ready", e.getMessage());
    }

    @ExceptionHandler(ConcurrencyLimitException.class)
    public ProblemDetail onConcurrency(ConcurrencyLimitException e) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Concurrency limit", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An unexpected error occurred");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
