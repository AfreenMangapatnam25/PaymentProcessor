package com.paymentprocessor.userservice.common.exception;



import com.paymentprocessor.userservice.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Abstract base class for all application-specific exceptions.
 * Extends RuntimeException to allow unchecked exception handling.
 */
public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final String message;

    /**
     * Constructor with all fields.
     *
     * @param errorCode   the error code enum
     * @param httpStatus  the HTTP status to return
     * @param message     the detailed error message
     */
    protected ApplicationException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * Constructor with error code and HTTP status, uses default message from error code.
     *
     * @param errorCode   the error code enum
     * @param httpStatus  the HTTP status to return
     */
    protected ApplicationException(ErrorCode errorCode, HttpStatus httpStatus) {
        this(errorCode, httpStatus, errorCode.getDefaultMessage());
    }

    /**
     * Constructor with error code only, maps to appropriate HTTP status.
     *
     * @param errorCode the error code enum
     */
    protected ApplicationException(ErrorCode errorCode) {
        this(errorCode, mapHttpStatus(errorCode), errorCode.getDefaultMessage());
    }

    /**
     * Constructor with error code and custom message, auto-maps HTTP status.
     *
     * @param errorCode the error code enum
     * @param message   the custom error message
     */
    protected ApplicationException(ErrorCode errorCode, String message) {
        this(errorCode, mapHttpStatus(errorCode), message);
    }

    /**
     * Constructor with error code, custom message, and cause.
     *
     * @param errorCode the error code enum
     * @param message   the custom error message
     * @param cause     the underlying cause
     */
    protected ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = mapHttpStatus(errorCode);
        this.message = message;
    }

    // ===== GETTERS =====

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the HTTP status.
     *
     * @return the HTTP status
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    @Override
    public String getMessage() {
        return message;
    }

    // ===== HELPER METHODS =====

    /**
     * Maps an error code to an appropriate HTTP status.
     *
     * @param errorCode the error code
     * @return the mapped HTTP status
     */
    private static HttpStatus mapHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            // 4xx Client Errors
            case USER_NOT_FOUND, CUSTOMER_NOT_FOUND, ADDRESS_NOT_FOUND, CONSENT_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;
            case USER_ALREADY_EXISTS, CUSTOMER_ALREADY_EXISTS,CONFLICT, MERCHANT_MISMATCH ->
                    HttpStatus.CONFLICT;
            case INVALID_EMAIL, INVALID_PHONE, INVALID_COUNTRY, VALIDATION_FAILED ->
                    HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED ->
                    HttpStatus.UNAUTHORIZED;
            case FORBIDDEN ->
                    HttpStatus.FORBIDDEN;

            // 5xx Server Errors
            case DATABASE_ERROR, INTERNAL_SERVER_ERROR, ENCRYPTION_ERROR, GDPR_ERASURE_FAILED ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Creates an error response DTO for API responses.
     *
     * @return ErrorResponse object
     */
    public ErrorCode.ErrorResponse toErrorResponse() {
        return new ErrorCode.ErrorResponse(
                errorCode.getCode(),
                this.message
        );
    }

    @Override
    public String toString() {
        return String.format(
                "ApplicationException{errorCode=%s, httpStatus=%s, message='%s'}",
                errorCode, httpStatus, message
        );
    }
}