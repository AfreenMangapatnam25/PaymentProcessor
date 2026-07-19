package com.paymentprocessor.userservice.domain.valueobject;

import com.paymentprocessor.userservice.common.exception.ValidationException;

/**
 * Opaque reference to a merchant owned by merchant-service. This service never
 * dereferences it, but every customer read/write is scoped by it to guarantee
 * tenant isolation (rule 12): two merchants must never see the same customer.
 */
public record MerchantId(String value) {

    public MerchantId {
        if (value == null || value.isBlank()) {
            throw new ValidationException("merchantId", "Merchant id must not be blank");
        }
        if (value.length() > 64) {
            throw new ValidationException("merchantId", "Merchant id must not exceed 64 characters");
        }
    }

    public static MerchantId of(String value) {
        return new MerchantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
