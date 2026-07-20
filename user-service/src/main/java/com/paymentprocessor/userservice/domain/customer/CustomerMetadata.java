package com.paymentprocessor.userservice.domain.customer;

import com.paymentprocessor.userservice.common.exception.ValidationException;

import java.util.Map;

/**
 * Free-form merchant metadata attached to a customer (non-PII by contract).
 * Immutable and bounded so a merchant cannot bloat a row. Stored as jsonb.
 */
public record CustomerMetadata(Map<String, String> values) {

    private static final int MAX_ENTRIES = 50;
    private static final int MAX_VALUE_LENGTH = 512;

    public CustomerMetadata {
        values = values == null ? Map.of() : Map.copyOf(values);
        if (values.size() > MAX_ENTRIES) {
            throw new ValidationException("metadata", "At most " + MAX_ENTRIES + " metadata entries are allowed");
        }
        for (String v : values.values()) {
            if (v != null && v.length() > MAX_VALUE_LENGTH) {
                throw new ValidationException("metadata", "Metadata value exceeds " + MAX_VALUE_LENGTH + " characters");
            }
        }
    }

    public static CustomerMetadata empty() {
        return new CustomerMetadata(Map.of());
    }

    public static CustomerMetadata of(Map<String, String> values) {
        return new CustomerMetadata(values);
    }
}
