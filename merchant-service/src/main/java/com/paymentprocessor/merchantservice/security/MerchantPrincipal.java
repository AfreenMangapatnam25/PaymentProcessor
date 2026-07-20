package com.paymentprocessor.merchantservice.security;

import com.paymentprocessor.merchantservice.common.enums.ApiKeyType;

import java.util.UUID;

/**
 * The authenticated caller. For a platform admin credential, {@code admin} is true and
 * {@code merchantId} is null. For a merchant API key, {@code merchantId} identifies the scope.
 */
public record MerchantPrincipal(
        UUID merchantId,
        String keyId,
        ApiKeyType keyType,
        boolean admin
) {
    public static MerchantPrincipal admin(String keyId) {
        return new MerchantPrincipal(null, keyId, null, true);
    }

    public static MerchantPrincipal merchant(UUID merchantId, String keyId, ApiKeyType keyType) {
        return new MerchantPrincipal(merchantId, keyId, keyType, false);
    }
}
