package com.paymentprocessor.settlementservice.integration.merchant;

import java.util.Optional;

/**
 * Read-only gateway to the Merchant Service. Swap the {@code Simulated}
 * implementation for an HTTP/gRPC-backed one in production without touching
 * the settlement domain.
 */
public interface MerchantClient {

    /** Returns the settlement profile for a merchant, or empty if unknown. */
    Optional<MerchantSettlementProfile> getProfile(String merchantId);

    /** Verifies the merchant's destination bank account is valid and payable. */
    boolean isPayoutAccountValid(String merchantId, String payoutAccountId);
}
