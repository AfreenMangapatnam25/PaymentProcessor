package com.paymentprocessor.authorization.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Currency-aware conversion between major-unit {@link BigDecimal} amounts and the minor-unit
 * {@code long} amounts most gateways (including Stripe) expect.
 */
public final class MoneyUtil {

    /** Currencies that have no minor unit (amounts are already integral). */
    private static final Set<String> ZERO_DECIMAL = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA",
            "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF");

    private MoneyUtil() {
    }

    public static int fractionDigits(String currency) {
        return ZERO_DECIMAL.contains(currency == null ? "" : currency.toUpperCase()) ? 0 : 2;
    }

    /** Convert a major-unit amount to the gateway minor-unit representation. */
    public static long toMinorUnits(BigDecimal amount, String currency) {
        int digits = fractionDigits(currency);
        return amount.movePointRight(digits).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** Convert a gateway minor-unit amount back to a major-unit {@link BigDecimal}. */
    public static BigDecimal fromMinorUnits(long minor, String currency) {
        int digits = fractionDigits(currency);
        return BigDecimal.valueOf(minor).movePointLeft(digits);
    }
}
