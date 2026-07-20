package com.paymentprocessor.userservice.common.exception;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Used for: User, Customer, Address, Consent
 */
public class ResourceNotFoundException extends ApplicationException {

    private final String resourceType;
    private final String resourceId;

    /**
     * Constructor with resource type and ID.
     *
     * @param resourceType the type of resource (e.g., "User", "Customer")
     * @param resourceId   the ID of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(
                mapErrorCode(resourceType),
                HttpStatus.NOT_FOUND,
                String.format("%s with ID '%s' not found", resourceType, resourceId)
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Constructor with resource type, ID, and custom message.
     */
    public ResourceNotFoundException(String resourceType, String resourceId, String message) {
        super(mapErrorCode(resourceType), HttpStatus.NOT_FOUND, message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Constructor with just error code (uses default message).
     */
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.NOT_FOUND);
        this.resourceType = extractResourceType(errorCode);
        this.resourceId = null;
    }

    /**
     * Constructor with error code and custom message.
     */
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
        this.resourceType = extractResourceType(errorCode);
        this.resourceId = null;
    }

    private static ErrorCode mapErrorCode(String resourceType) {
        return switch (resourceType.toLowerCase()) {
            case "user" -> ErrorCode.USER_NOT_FOUND;
            case "customer" -> ErrorCode.CUSTOMER_NOT_FOUND;
            case "address" -> ErrorCode.ADDRESS_NOT_FOUND;
            case "consent" -> ErrorCode.CONSENT_NOT_FOUND;
            default -> ErrorCode.INTERNAL_SERVER_ERROR;
        };
    }

    private static String extractResourceType(ErrorCode errorCode) {
        return switch (errorCode) {
            case USER_NOT_FOUND -> "User";
            case CUSTOMER_NOT_FOUND -> "Customer";
            case ADDRESS_NOT_FOUND -> "Address";
            case CONSENT_NOT_FOUND -> "Consent";
            default -> "Resource";
        };
    }

    // Getters
    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}