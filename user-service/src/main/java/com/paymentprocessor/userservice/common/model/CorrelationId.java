package com.paymentprocessor.userservice.common.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Record representing a correlation ID for distributed tracing.
 * Correlation IDs are used to track requests across multiple services.
 */
public record CorrelationId(String value) implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PREFIX = "cor_";
    private static final int RANDOM_LENGTH = 16;

    /**
     * Canonical constructor with validation.
     *
     * @param value the correlation ID value
     * @throws IllegalArgumentException if the value is null, blank, or invalid format
     */
    public CorrelationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or blank");
        }
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid correlation ID format: " + value);
        }
    }

    /**
     * Creates a new CorrelationId with a generated value.
     *
     * @return a new CorrelationId instance
     */
    public static CorrelationId generate() {
        String value = PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, RANDOM_LENGTH);
        return new CorrelationId(value);
    }

    /**
     * Creates a CorrelationId from a string value.
     *
     * @param value the correlation ID string
     * @return a CorrelationId instance
     * @throws IllegalArgumentException if the value is invalid
     */
    public static CorrelationId fromString(String value) {
        return new CorrelationId(value);
    }

    /**
     * Creates a CorrelationId from a string value, returning null if invalid.
     *
     * @param value the correlation ID string
     * @return a CorrelationId instance or null if invalid
     */
    public static CorrelationId fromStringOrNull(String value) {
        try {
            return value != null ? new CorrelationId(value) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Validates a correlation ID string.
     *
     * @param value the correlation ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        // Must start with cor_ and have at least some characters after
        return value.startsWith(PREFIX) && value.length() > PREFIX.length();
    }

    /**
     * Checks if this correlation ID equals another by comparing values.
     *
     * @param other the other correlation ID
     * @return true if equal
     */
    public boolean matches(CorrelationId other) {
        return other != null && this.value.equals(other.value());
    }

    /**
     * Checks if this correlation ID equals a string value.
     *
     * @param otherValue the string value to compare
     * @return true if equal
     */
    public boolean matches(String otherValue) {
        return otherValue != null && this.value.equals(otherValue);
    }

    /**
     * Returns the correlation ID as a string.
     *
     * @return the string value
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Returns the correlation ID without the prefix.
     *
     * @return the raw correlation ID
     */
    public String getRawValue() {
        return value.substring(PREFIX.length());
    }

    /**
     * Gets the prefix of this correlation ID.
     *
     * @return the prefix ("cor_")
     */
    public String getPrefix() {
        return PREFIX;
    }

    /**
     * Creates a new CorrelationId with a custom prefix.
     *
     * @param newPrefix the new prefix (without trailing underscore)
     * @return a new CorrelationId instance
     */
    public CorrelationId withPrefix(String newPrefix) {
        String cleanPrefix = newPrefix.replaceAll("_$", "");
        String raw = getRawValue();
        return new CorrelationId(cleanPrefix + "_" + raw);
    }

    /**
     * Checks if this correlation ID is from the same request as another.
     * Useful for comparing correlation IDs that might have different prefixes.
     *
     * @param other the other correlation ID
     * @return true if the raw values match
     */
    public boolean sameRequest(CorrelationId other) {
        return other != null && this.getRawValue().equals(other.getRawValue());
    }
}