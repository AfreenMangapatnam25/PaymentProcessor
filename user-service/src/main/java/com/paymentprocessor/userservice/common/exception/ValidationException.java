package com.paymentprocessor.userservice.common.exception;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exception thrown when validation fails.
 * Includes a map of field-specific validation errors.
 */
public class ValidationException extends ApplicationException {

    private final Map<String, String> validationErrors;

    /**
     * Constructor with validation errors map.
     */
    public ValidationException(Map<String, String> validationErrors) {
        super(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                formatValidationMessage(validationErrors)
        );
        this.validationErrors = validationErrors != null
                ? new HashMap<>(validationErrors)
                : new HashMap<>();
    }

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
        this.validationErrors = new HashMap<>();
    }
    /**
     * Constructor with validation errors and custom message.
     */
    public ValidationException(Map<String, String> validationErrors, String message) {
        super(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
        this.validationErrors = validationErrors != null
                ? new HashMap<>(validationErrors)
                : new HashMap<>();
    }

    /**
     * Constructor with single field validation error.
     */
    public ValidationException(String field, String error) {
        super(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                String.format("Validation failed for field '%s': %s", field, error));
        this.validationErrors = new HashMap<>();
        this.validationErrors.put(field, error);
    }

    /**
     * Constructor with error code and validation errors.
     */
    public ValidationException(ErrorCode errorCode, Map<String, String> validationErrors) {
        super(errorCode, HttpStatus.BAD_REQUEST);
        this.validationErrors = validationErrors != null
                ? new HashMap<>(validationErrors)
                : new HashMap<>();
    }

    private static String formatValidationMessage(Map<String, String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed";
        }
        return errors.entrySet().stream()
                .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("; "));
    }

    /**
     * Adds a validation error to the map.
     */
    public void addValidationError(String field, String error) {
        this.validationErrors.put(field, error);
    }

    /**
     * Checks if there are any validation errors.
     */
    public boolean hasErrors() {
        return !validationErrors.isEmpty();
    }

    // Getter
    public Map<String, String> getValidationErrors() {
        return new HashMap<>(validationErrors);
    }

    @Override
    public String toString() {
        return String.format(
                "ValidationException{errorCode=%s, httpStatus=%s, message='%s', validationErrors=%s}",
                getErrorCode(), getHttpStatus(), getMessage(), validationErrors
        );
    }
}