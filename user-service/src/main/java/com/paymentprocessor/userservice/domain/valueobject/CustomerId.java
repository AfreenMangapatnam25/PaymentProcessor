package com.paymentprocessor.userservice.domain.valueobject;

import com.paymentprocessor.userservice.common.exception.ValidationException;

/**
 * Identity of a merchant-scoped {@code Customer} ({@code cus_<ULID>}).
 */
public record CustomerId(String value) {

    public static final String PREFIX = "cus_";

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new ValidationException("customerId", "Customer id must not be blank");
        }
        if (!value.startsWith(PREFIX)) {
            throw new ValidationException("customerId", "Customer id must start with '" + PREFIX + "'");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
