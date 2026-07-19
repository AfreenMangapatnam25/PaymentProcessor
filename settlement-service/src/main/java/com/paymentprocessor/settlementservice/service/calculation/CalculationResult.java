package com.paymentprocessor.settlementservice.service.calculation;

/**
 * The immutable outcome of a settlement calculation for one merchant/currency
 * period. All values are minor units and follow the README formula:
 *
 * <pre>
 * net = gross − refunds − chargebacks − platformFee − interchange
 *             − reserve − settlementFee + adjustments
 * </pre>
 */
public record CalculationResult(
        String currency,
        long grossMinor,
        long refundsMinor,
        long chargebacksMinor,
        long platformFeeMinor,
        long interchangeMinor,
        long reserveMinor,
        long settlementFeeMinor,
        long adjustmentsMinor,
        long netMinor
) {
    /** Total of platform + settlement fees, for reporting convenience. */
    public long totalFeesMinor() {
        return platformFeeMinor + settlementFeeMinor;
    }
}
