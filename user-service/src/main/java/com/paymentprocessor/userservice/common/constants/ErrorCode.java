package com.paymentprocessor.userservice.common.constants;

/**
 * Enum representing all possible error codes in the system.
 * Each error code includes a default message for consistent error handling.
 */
public enum ErrorCode {

    // User errors
    USER_NOT_FOUND("User not found"),
    CUSTOMER_NOT_FOUND("Customer not found"),
    ADDRESS_NOT_FOUND("Address not found"),
    CONSENT_NOT_FOUND("Consent record not found"),

    // Duplicate/Conflict errors
    USER_ALREADY_EXISTS("User already exists"),
    CUSTOMER_ALREADY_EXISTS("Customer already exists"),
    CONFLICT("Conflict - resource already exists"),

    // Validation errors
    INVALID_EMAIL("Invalid email address format"),
    INVALID_PHONE("Invalid phone number format"),
    INVALID_COUNTRY("Invalid country code"),

    // Authorization errors
    MERCHANT_MISMATCH("Merchant ID mismatch - resource does not belong to this merchant"),
    FORBIDDEN("Access forbidden - insufficient permissions"),
    UNAUTHORIZED("Unauthorized - authentication required"),
    VALIDATION_FAILED("Validation failed - one or more fields are invalid"),

    // System errors
    DATABASE_ERROR("Database operation failed"),
    INTERNAL_SERVER_ERROR("Internal server error occurred"),
    ENCRYPTION_ERROR("Encryption/decryption operation failed"),
    GDPR_ERASURE_FAILED("GDPR data erasure request failed");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    /**
     * Gets the default error message for this error code.
     *
     * @return the default error message
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Gets the error code as a string (the enum name).
     *
     * @return the error code string
     */
    public String getCode() {
        return this.name();
    }

    /**
     * Creates a standard error response object for this error code.
     * Useful for consistent API error responses.
     *
     * @return an ErrorResponse object containing the error code and default message
     */
    public ErrorResponse toErrorResponse() {
        return new ErrorResponse(this.name(), this.defaultMessage);
    }

    /**
     * Checks if this error code represents a client-side error (4xx).
     *
     * @return true if client error, false otherwise
     */
    public boolean isClientError() {
        return switch (this) {
            case USER_NOT_FOUND, CUSTOMER_NOT_FOUND, ADDRESS_NOT_FOUND,
                 CONSENT_NOT_FOUND, INVALID_EMAIL, INVALID_PHONE, INVALID_COUNTRY,
                 MERCHANT_MISMATCH, FORBIDDEN, UNAUTHORIZED, VALIDATION_FAILED,CONFLICT,
                 USER_ALREADY_EXISTS, CUSTOMER_ALREADY_EXISTS -> true;
            default -> false;
        };
    }

    /**
     * Checks if this error code represents a server-side error (5xx).
     *
     * @return true if server error, false otherwise
     */
    public boolean isServerError() {
        return !isClientError();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", this.name(), this.defaultMessage);
    }

    /**
         * Inner class for standard error response objects.
         */
        public record ErrorResponse(String code, String message) {

        @Override
            public String toString() {
                return String.format("{\"code\":\"%s\",\"message\":\"%s\"}", code, message);
            }
        }
}