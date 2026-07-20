package com.paymentprocessor.userservice.domain.valueobject;

import com.paymentprocessor.userservice.common.exception.ValidationException;

/**
 * Identity of a platform {@code User}. Immutable value object wrapping a
 * prefixed ULID ({@code usr_<ULID>}). Constructing one enforces the prefix so
 * an id from the wrong aggregate can never slip through the type system.
 */
public record UserId(String value) {

    public static final String PREFIX = "usr_";

    public UserId {
        if (value == null || value.isBlank()) {
            throw new ValidationException("userId", "User id must not be blank");
        }
        if (!value.startsWith(PREFIX)) {
            throw new ValidationException("userId", "User id must start with '" + PREFIX + "'");
        }
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
