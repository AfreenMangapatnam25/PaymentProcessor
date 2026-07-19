package com.paymentprocessor.userservice.domain.valueobject;

import com.paymentprocessor.userservice.common.exception.ValidationException;

/**
 * Opaque reference to a principal in authentication-service. This service never
 * interprets or dereferences the value; it only stores and matches on it.
 */
public record IdentityId(String value) {

    public IdentityId {
        if (value == null || value.isBlank()) {
            throw new ValidationException("identityId", "Identity id must not be blank");
        }
        if (value.length() > 128) {
            throw new ValidationException("identityId", "Identity id must not exceed 128 characters");
        }
    }

    public static IdentityId of(String value) {
        return new IdentityId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
