package com.paymentprocessor.ledgerservice.event;

import java.time.Instant;

/**
 * Published when an account's balance changes due to a posting, reversal, or hold.
 */
public record BalanceUpdatedEvent(
        String accountId,
        String currency,
        long postedMinor,
        long heldMinor,
        long availableMinor,
        long entryHighWater,
        Instant updatedAt
) {
    public static final String TYPE = "BalanceUpdated";
}
