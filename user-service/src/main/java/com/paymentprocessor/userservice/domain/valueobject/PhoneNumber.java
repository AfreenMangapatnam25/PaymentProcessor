package com.paymentprocessor.userservice.domain.valueobject;

import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.common.validation.PhoneValidator;

/**
 * Phone number in normalized E.164 form. PII: encrypted at rest, masked in
 * logs. Construct only when a phone is actually present (optional field).
 */
public record PhoneNumber(String value) {

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new ValidationException("phone", "Phone must not be blank");
        }
        value = PhoneValidator.normalize(value);
        if (!PhoneValidator.validateE164(value)) {
            throw new ValidationException("phone", "Phone must be a valid E.164 number");
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    @Override
    public String toString() {
        return "PhoneNumber{***}";
    }
}
