package com.paymentprocessor.settlementservice.service.calculation;

import com.paymentprocessor.settlementservice.common.Money;
import com.paymentprocessor.settlementservice.entity.SettlementItem;
import com.paymentprocessor.settlementservice.enums.SettlementItemType;
import com.paymentprocessor.settlementservice.exception.BadRequestException;
import com.paymentprocessor.settlementservice.integration.merchant.MerchantSettlementProfile;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Pure, side-effect-free settlement calculation engine. Given the raw line
 * items for a merchant/currency period plus the merchant's pricing profile and
 * the rail transfer fee, it produces the full monetary breakdown and net payout.
 *
 * <p>All arithmetic is performed in integer minor units via {@link Money} to
 * eliminate floating-point rounding error.
 */
@Service
public class CalculationService {

    /**
     * @param items            all line items for the period (must share a currency)
     * @param profile          merchant pricing / reserve configuration
     * @param settlementFeeMinor the rail transfer fee for this payout (minor units)
     * @return the calculated breakdown and net settlement
     */
    public CalculationResult calculate(List<SettlementItem> items,
                                       MerchantSettlementProfile profile,
                                       long settlementFeeMinor) {
        String currency = profile.settlementCurrency();
        validateCurrency(items, currency);

        Money gross = Money.zero(currency);
        Money refunds = Money.zero(currency);
        Money chargebacks = Money.zero(currency);
        Money interchange = Money.zero(currency);
        Money adjustments = Money.zero(currency);
        long captureCount = 0;

        for (SettlementItem item : items) {
            long amount = item.getAmountMinor();
            SettlementItemType type = item.getType();
            switch (type) {
                case CAPTURE -> {
                    gross = gross.plus(amount);
                    captureCount++;
                }
                case REFUND -> refunds = refunds.plus(amount);
                case CHARGEBACK -> chargebacks = chargebacks.plus(amount);
                case CHARGEBACK_REVERSAL -> chargebacks = chargebacks.minus(amount);
                case INTERCHANGE_FEE -> interchange = interchange.plus(amount);
                case ADJUSTMENT_CREDIT, ADJUSTMENT_DEBIT, RESERVE_RELEASE, PRIOR_PERIOD_CORRECTION ->
                        adjustments = adjustments.plus(item.signedAmountMinor());
                // Platform, settlement fees and reserve holds are computed here,
                // not ingested as items, so they are ignored if present.
                case PLATFORM_FEE, SETTLEMENT_FEE, RESERVE_HOLD -> { /* computed below */ }
            }
        }

        // Platform fee = percentage of gross + fixed per captured transaction.
        Money platformFee = gross.percentageBps(profile.platformFeeBps())
                .plus(Math.multiplyExact(profile.platformFeeFixedMinor(), captureCount));

        // Rolling reserve = rate applied to net sales (gross − refunds), never negative.
        Money netSales = gross.minus(refunds);
        Money reserve = netSales.isPositive()
                ? netSales.percentageBps(profile.reserveRateBps())
                : Money.zero(currency);

        Money settlementFee = profile.passThroughSettlementFee()
                ? Money.of(settlementFeeMinor, currency)
                : Money.zero(currency);

        Money net = gross
                .minus(refunds)
                .minus(chargebacks)
                .minus(platformFee)
                .minus(interchange)
                .minus(reserve)
                .minus(settlementFee)
                .plus(adjustments);

        return new CalculationResult(
                currency,
                gross.amountMinor(),
                refunds.amountMinor(),
                chargebacks.amountMinor(),
                platformFee.amountMinor(),
                interchange.amountMinor(),
                reserve.amountMinor(),
                settlementFee.amountMinor(),
                adjustments.amountMinor(),
                net.amountMinor());
    }

    private void validateCurrency(List<SettlementItem> items, String currency) {
        for (SettlementItem item : items) {
            if (!currency.equalsIgnoreCase(item.getCurrency())) {
                throw new BadRequestException(
                        "Settlement item " + item.getId() + " currency " + item.getCurrency()
                                + " does not match batch currency " + currency);
            }
        }
    }
}
