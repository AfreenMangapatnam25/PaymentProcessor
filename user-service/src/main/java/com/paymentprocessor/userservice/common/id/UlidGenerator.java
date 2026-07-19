package com.paymentprocessor.userservice.common.id;


import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * ULID-based ID generator implementation.
 * Generates lexicographically sortable, unique IDs with entity-specific prefixes.
 */
@Component
public class UlidGenerator implements IdGenerator {

    private static final String USER_PREFIX = "usr_";
    private static final String CUSTOMER_PREFIX = "cus_";
    private static final String ADDRESS_PREFIX = "adr_";
    private static final String CONSENT_PREFIX = "con_";
    private static final String OUTBOX_PREFIX = "out_";

    private final Ulid ulid;

    /**
     * Default constructor - creates a new ULID generator.
     */
    public UlidGenerator() {
        this.ulid = UlidCreator.getUlid();
    }

    /**
     * Constructor with custom ULID.
     *
     * @param ulid the ULID instance to use
     */
    public UlidGenerator(Ulid ulid) {
        this.ulid = ulid;
    }

    @Override
    public String generateUserId() {
        return USER_PREFIX + generateRawUlid();
    }

    @Override
    public String generateCustomerId() {
        return CUSTOMER_PREFIX + generateRawUlid();
    }

    @Override
    public String generateAddressId() {
        return ADDRESS_PREFIX + generateRawUlid();
    }

    @Override
    public String generateConsentId() {
        return CONSENT_PREFIX + generateRawUlid();
    }

    @Override
    public String generateOutboxId() {
        return OUTBOX_PREFIX + generateRawUlid();
    }

    @Override
    public String generateRawUlid() {
        return ulid.toString();
    }

    @Override
    public String generatePrefixedId(String prefix) {
        // Remove any existing prefix or underscores
        String cleanPrefix = prefix.replaceAll("_$", "").toLowerCase();
        return cleanPrefix + "_" + generateRawUlid();
    }

    /**
     * Generates a ULID with a specific timestamp.
     * Useful for testing or deterministic generation.
     *
     * @param instant the timestamp to use
     * @return ULID string
     */
    public String generateUlid(Instant instant) {
        return UlidCreator.getUlid(instant.toEpochMilli()).toString();
    }

    /**
     * Generates a prefixed ULID with a specific timestamp.
     *
     * @param prefix  the prefix to use
     * @param instant the timestamp to use
     * @return prefixed ULID string
     */
    public String generatePrefixedId(String prefix, Instant instant) {
        String cleanPrefix = prefix.replaceAll("_$", "").toLowerCase();
        return cleanPrefix + "_" + generateUlid(instant);
    }

    /**
     * Extracts the raw ULID from a prefixed ID.
     *
     * @param prefixedId the prefixed ID
     * @return the raw ULID portion
     * @throws IllegalArgumentException if the ID format is invalid
     */
    public static String extractUlid(String prefixedId) {
        if (prefixedId == null || !prefixedId.contains("_")) {
            throw new IllegalArgumentException("Invalid prefixed ID format: " + prefixedId);
        }
        return prefixedId.substring(prefixedId.indexOf('_') + 1);
    }

    /**
     * Extracts the prefix from a prefixed ID.
     *
     * @param prefixedId the prefixed ID
     * @return the prefix portion
     * @throws IllegalArgumentException if the ID format is invalid
     */
    public static String extractPrefix(String prefixedId) {
        if (prefixedId == null || !prefixedId.contains("_")) {
            throw new IllegalArgumentException("Invalid prefixed ID format: " + prefixedId);
        }
        return prefixedId.substring(0, prefixedId.indexOf('_'));
    }

    /**
     * Validates if a string is a valid prefixed ID.
     *
     * @param id the ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPrefixedId(String id) {
        if (id == null || !id.contains("_")) {
            return false;
        }
        String raw = extractUlid(id);
        return raw.length() == 26 && raw.matches("[0-9A-Z]+");
    }

    /**
     * Gets the timestamp from a prefixed ID.
     *
     * @param prefixedId the prefixed ID
     * @return the timestamp instant
     */
    public static Instant getTimestamp(String prefixedId) {
        String raw = extractUlid(prefixedId);
        return Ulid.from(raw).getInstant();
    }
}
