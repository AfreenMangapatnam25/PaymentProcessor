package com.paymentprocessor.userservice.common.id;

/**
 * Interface for generating unique identifiers for different entity types.
 * All IDs are prefixed with entity-specific codes for easy identification.
 */
public interface IdGenerator {

    /**
     * Generates a unique user ID with 'usr_' prefix.
     * Format: usr_01KXXXXXXXXXXXXXX
     *
     * @return unique user ID
     */
    String generateUserId();

    /**
     * Generates a unique customer ID with 'cus_' prefix.
     * Format: cus_01KXXXXXXXXXXXXXX
     *
     * @return unique customer ID
     */
    String generateCustomerId();

    /**
     * Generates a unique address ID with 'adr_' prefix.
     * Format: adr_01KXXXXXXXXXXXXXX
     *
     * @return unique address ID
     */
    String generateAddressId();

    /**
     * Generates a unique consent ID with 'con_' prefix.
     * Format: con_01KXXXXXXXXXXXXXX
     *
     * @return unique consent ID
     */
    String generateConsentId();

    /**
     * Generates a unique outbox ID with 'out_' prefix.
     * Format: out_01KXXXXXXXXXXXXXX
     *
     * @return unique outbox ID
     */
    String generateOutboxId();

    /**
     * Generates a raw ULID without prefix.
     *
     * @return raw ULID string
     */
    String generateRawUlid();

    /**
     * Generates a custom prefixed ID.
     *
     * @param prefix the prefix to use (e.g., "usr", "cus")
     * @return prefixed ULID
     */
    String generatePrefixedId(String prefix);
}
