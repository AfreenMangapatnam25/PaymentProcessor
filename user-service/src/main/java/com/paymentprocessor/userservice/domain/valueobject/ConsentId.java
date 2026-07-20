package com.paymentprocessor.userservice.domain.valueobject;

import com.paymentprocessor.userservice.common.exception.ValidationException;

/**
 * Identity of a {@code Consent} ({@code con_<ULID>}).
 */
public record ConsentId(String value) {

    public static final String PREFIX = "con_";

    public ConsentId {
        if (value == null || value.isBlank()) {
            throw new ValidationException("consentId", "Consent id must not be blank");
        }
        if (!value.startsWith(PREFIX)) {
            throw new ValidationException("consentId", "Consent id must start with '" + PREFIX + "'");
        }
    }

    public static ConsentId of(String value) {
        return new ConsentId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
