package com.paymentprocessor.ledgerservice.domain.enums;

/**
 * Well-known ledger account purposes used by settlement-service and dispute-service
 * when resolving account ids via {@code GET /api/v1/accounts/resolve}.
 */
public enum AccountPurpose {
    PLATFORM_CASH,
    PLATFORM_FEE_REVENUE,
    PLATFORM_PAYOUT_PAYABLE,
    PLATFORM_ADJUSTMENT_EXPENSE,
    PLATFORM_CHARGEBACK_CLEARING,
    PLATFORM_FEE_EXPENSE,
    MERCHANT_SETTLEMENT_LIABILITY,
    MERCHANT_RESERVE
}
