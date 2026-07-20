package com.paymentprocessor.userservice.domain.valueobject;

import com.paymentprocessor.userservice.common.exception.ValidationException;

/**
 * Identity of an {@code Address} ({@code adr_<ULID>}).
 */
public record AddressId(String value) {

    public static final String PREFIX = "adr_";

    public AddressId {
        if (value == null || value.isBlank()) {
            throw new ValidationException("addressId", "Address id must not be blank");
        }
        if (!value.startsWith(PREFIX)) {
            throw new ValidationException("addressId", "Address id must start with '" + PREFIX + "'");
        }
    }

    public static AddressId of(String value) {
        return new AddressId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
