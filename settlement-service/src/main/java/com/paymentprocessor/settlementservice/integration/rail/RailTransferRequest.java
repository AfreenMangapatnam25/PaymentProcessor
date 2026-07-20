package com.paymentprocessor.settlementservice.integration.rail;

import com.paymentprocessor.settlementservice.enums.Rail;

/**
 * A request to move funds to a merchant account over a specific rail.
 *
 * @param payoutId        settlement payout id (used for tracing)
 * @param rail            the selected rail
 * @param merchantId      merchant identifier
 * @param payoutAccountId destination account identifier
 * @param amountMinor     amount to transfer, minor units
 * @param currency        ISO-4217 currency code
 * @param idempotencyKey  ensures the rail does not double-submit
 */
public record RailTransferRequest(
        String payoutId,
        Rail rail,
        String merchantId,
        String payoutAccountId,
        long amountMinor,
        String currency,
        String idempotencyKey
) {
}
