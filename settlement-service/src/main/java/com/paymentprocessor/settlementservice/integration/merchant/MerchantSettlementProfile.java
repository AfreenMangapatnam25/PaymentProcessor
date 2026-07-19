package com.paymentprocessor.settlementservice.integration.merchant;

import com.paymentprocessor.settlementservice.enums.Rail;
import com.paymentprocessor.settlementservice.enums.ScheduleType;

/**
 * A snapshot of the settlement-relevant merchant configuration owned by the
 * Merchant Service. The Settlement Service reads (never writes) this data.
 *
 * @param merchantId               merchant identifier
 * @param active                   whether the merchant account is ACTIVE and eligible for payout
 * @param settlementCurrency       ISO-4217 currency the merchant is paid in
 * @param scheduleType             payout schedule frequency
 * @param payoutAccountId          identifier of the destination bank account
 * @param platformFeeBps           platform fee rate in basis points (e.g. 290 = 2.9%)
 * @param platformFeeFixedMinor    fixed platform fee per captured transaction, minor units
 * @param reserveRateBps           rolling reserve rate in basis points (e.g. 1000 = 10%)
 * @param reserveHoldDays          number of days a reserve is held before release
 * @param passThroughSettlementFee whether the rail transfer fee is deducted from the merchant payout
 * @param preferredRail            an optional forced rail; {@code null} means auto-select
 * @param instantPayout            whether the merchant is entitled to instant payouts
 */
public record MerchantSettlementProfile(
        String merchantId,
        boolean active,
        String settlementCurrency,
        ScheduleType scheduleType,
        String payoutAccountId,
        int platformFeeBps,
        long platformFeeFixedMinor,
        int reserveRateBps,
        int reserveHoldDays,
        boolean passThroughSettlementFee,
        Rail preferredRail,
        boolean instantPayout
) {
}
