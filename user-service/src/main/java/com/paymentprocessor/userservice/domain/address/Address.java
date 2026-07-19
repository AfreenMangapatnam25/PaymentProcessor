package com.paymentprocessor.userservice.domain.address;

import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.domain.valueobject.AddressId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Address aggregate root, owned by a user or a customer (polymorphic
 * {@link AddressOwner}). The street lines/city/region/postal code are PII and
 * encrypted at rest with the OWNER's DEK; {@code countryCode} is not PII and
 * stays in clear for tax/routing. Invariants (required line1/city/postal code,
 * ISO country code, immutability once deleted) live here (rule 4).
 */
@Getter
@Builder(toBuilder = true)
public class Address {

    private final AddressId id;
    private final AddressOwner owner;
    private AddressType addressType;
    private String line1;
    private String line2;   // optional
    private String city;
    private String region;  // optional
    private String postalCode;
    private String countryCode;
    private boolean defaultAddress;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    @Builder.Default
    private long version = 0L;

    public static Address register(AddressId id,
                                   AddressOwner owner,
                                   AddressType addressType,
                                   String line1,
                                   String line2,
                                   String city,
                                   String region,
                                   String postalCode,
                                   String countryCode,
                                   boolean defaultAddress,
                                   Instant now) {
        return Address.builder()
                .id(id)
                .owner(owner)
                .addressType(addressType != null ? addressType : AddressType.SHIPPING)
                .line1(requireLine1(line1))
                .line2(trimToNull(line2))
                .city(requireCity(city))
                .region(trimToNull(region))
                .postalCode(requirePostalCode(postalCode))
                .countryCode(requireCountry(countryCode))
                .defaultAddress(defaultAddress)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    public void update(AddressType addressType, String line1, String line2, String city,
                       String region, String postalCode, String countryCode) {
        assertMutable();
        this.addressType = addressType != null ? addressType : this.addressType;
        this.line1 = requireLine1(line1);
        this.line2 = trimToNull(line2);
        this.city = requireCity(city);
        this.region = trimToNull(region);
        this.postalCode = requirePostalCode(postalCode);
        this.countryCode = requireCountry(countryCode);
        touch();
    }

    public void markDefault() {
        assertMutable();
        this.defaultAddress = true;
        touch();
    }

    public void clearDefault() {
        assertMutable();
        this.defaultAddress = false;
        touch();
    }

    public void softDelete(Instant now) {
        if (isDeleted()) {
            throw new ValidationException("status", "Address already deleted");
        }
        this.defaultAddress = false;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void assertMutable() {
        if (isDeleted()) {
            throw new ValidationException("status", "Deleted addresses are immutable");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String requireLine1(String v) {
        if (v == null || v.isBlank()) {
            throw new ValidationException("line1", "Address line 1 is required");
        }
        return checkLength(v.trim(), "line1", 255);
    }

    private static String requireCity(String v) {
        if (v == null || v.isBlank()) {
            throw new ValidationException("city", "City is required");
        }
        return checkLength(v.trim(), "city", 128);
    }

    private static String requirePostalCode(String v) {
        if (v == null || v.isBlank()) {
            throw new ValidationException("postalCode", "Postal code is required");
        }
        return checkLength(v.trim(), "postalCode", 32);
    }

    private static String requireCountry(String v) {
        if (v == null || !v.matches("^[A-Z]{2}$")) {
            throw new ValidationException("countryCode", "Country must be an ISO-3166-1 alpha-2 code");
        }
        return v;
    }

    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : checkLength(t, "line", 255);
    }

    private static String checkLength(String v, String field, int max) {
        if (v.length() > max) {
            throw new ValidationException(field, field + " must not exceed " + max + " characters");
        }
        return v;
    }
}
