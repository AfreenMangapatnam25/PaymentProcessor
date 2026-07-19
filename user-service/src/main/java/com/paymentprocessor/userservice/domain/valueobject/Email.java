package com.paymentprocessor.userservice.domain.valueobject;

import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.common.validation.EmailValidator;

/**
 * Email address value object. The value is normalized (trimmed, lower-cased,
 * provider-specific canonicalization) on construction so that equality and the
 * downstream blind index are stable. This is PII: it is only ever held in
 * memory transiently and is encrypted at rest.
 */
public record Email(String value) {

    public Email {
        if (value == null || value.isBlank()) {
            throw new ValidationException("email", "Email must not be blank");
        }
        value = EmailValidator.normalize(value);
        if (!EmailValidator.validate(value)) {
            throw new ValidationException("email", "Email format is invalid");
        }
        if (value.length() > 320) {
            throw new ValidationException("email", "Email must not exceed 320 characters");
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        // Do not leak PII through toString(); mask instead.
        return "Email{***}";
    }
}
