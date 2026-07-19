package com.paymentprocessor.settlementservice.integration.rail;

import com.paymentprocessor.settlementservice.enums.Rail;
import com.paymentprocessor.settlementservice.exception.BadRequestException;
import org.springframework.stereotype.Component;

/**
 * Selects the optimal rail for a payout based on amount, currency, and urgency,
 * following the rail-selection matrix in the service README.
 */
@Component
public class RailRouter {

    private static final long LARGE_TRANSFER_MINOR = 10_000_000L; // 100,000.00

    /**
     * @param currency ISO-4217 currency
     * @param amountMinor payout amount, minor units
     * @param instant whether the merchant is entitled to / requesting instant payout
     * @param preferred an optional forced rail; validated for currency support
     */
    public Rail selectRail(String currency, long amountMinor, boolean instant, Rail preferred) {
        if (preferred != null) {
            if (!preferred.supportsCurrency(currency)) {
                throw new BadRequestException(
                        "Preferred rail " + preferred + " does not support currency " + currency);
            }
            return preferred;
        }

        String ccy = currency == null ? "" : currency.toUpperCase();

        // Large-value transfers always go over Wire / SWIFT regardless of currency.
        if (amountMinor > LARGE_TRANSFER_MINOR) {
            return switch (ccy) {
                case "USD" -> Rail.WIRE;
                case "GBP" -> Rail.CHAPS;
                default -> Rail.SWIFT;
            };
        }

        return switch (ccy) {
            case "USD" -> instant ? Rail.RTP : Rail.ACH;
            case "EUR" -> instant ? Rail.SEPA_INSTANT : Rail.SEPA;
            case "GBP" -> instant ? Rail.FPS : Rail.BACS;
            default -> Rail.SWIFT;
        };
    }
}
