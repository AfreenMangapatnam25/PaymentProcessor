package com.paymentprocessor.userservice.common.exception;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a conflict occurs (duplicate data).
 * Used for: Duplicate Email, Duplicate External Ref, Duplicate Consent
 */
public class ConflictException extends ApplicationException {

    private final String conflictingField;
    private final String conflictingValue;
    private final String resourceType;

    /**
     * Constructor with conflicting field and value.
     */
    public ConflictException(String resourceType, String conflictingField, String conflictingValue) {
        super(
                mapErrorCode(resourceType, conflictingField),
                HttpStatus.CONFLICT,
                String.format("%s with %s '%s' already exists",
                        resourceType, conflictingField, conflictingValue)
        );
        this.resourceType = resourceType;
        this.conflictingField = conflictingField;
        this.conflictingValue = conflictingValue;
    }

    /**
     * Constructor with just error code.
     */
    public ConflictException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.CONFLICT);
        this.resourceType = null;
        this.conflictingField = null;
        this.conflictingValue = null;
    }

    /**
     * Constructor with error code and custom message.
     */
    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
        this.resourceType = null;
        this.conflictingField = null;
        this.conflictingValue = null;
    }

    /**
     * Constructor for duplicate email specifically.
     */
    public static ConflictException duplicateEmail(String email) {
        return new ConflictException(
                "User",
                "email",
                email
        );
    }

    /**
     * Constructor for duplicate external reference specifically.
     */
    public static ConflictException duplicateExternalRef(String externalRef) {
        return new ConflictException(
                "Customer",
                "external reference",
                externalRef
        );
    }

    /**
     * Constructor for duplicate consent specifically.
     */
    public static ConflictException duplicateConsent(String consentId) {
        return new ConflictException(
                "Consent",
                "consent ID",
                consentId
        );
    }

    private static ErrorCode mapErrorCode(String resourceType, String field) {
        if ("email".equalsIgnoreCase(field) || "User".equalsIgnoreCase(resourceType)) {
            return ErrorCode.USER_ALREADY_EXISTS;
        } else if ("external reference".equalsIgnoreCase(field) || "Customer".equalsIgnoreCase(resourceType)) {
            return ErrorCode.CUSTOMER_ALREADY_EXISTS;
        } else if ("consent".equalsIgnoreCase(resourceType) || "consent ID".equalsIgnoreCase(field)) {
            return ErrorCode.CONFLICT; // You might want to add CONSENT_ALREADY_EXISTS to ErrorCode enum
        }
        return ErrorCode.CONFLICT;
    }

    // Getters
    public String getConflictingField() {
        return conflictingField;
    }

    public String getConflictingValue() {
        return conflictingValue;
    }

    public String getResourceType() {
        return resourceType;
    }
}