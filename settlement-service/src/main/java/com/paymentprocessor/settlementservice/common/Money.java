package com.paymentprocessor.settlementservice.common;

import java.util.Objects;

/**
 * Immutable money value object stored in <em>minor units</em> (e.g. cents) with
 * an ISO-4217 currency code. All settlement arithmetic is performed on integers
 * to avoid floating-point rounding errors.
 */
public final class Money {

    private final long amountMinor;
    private final String currency;

    private Money(long amountMinor, String currency) {
        this.amountMinor = amountMinor;
        this.currency = Objects.requireNonNull(currency, "currency").toUpperCase();
    }

    public static Money of(long amountMinor, String currency) {
        return new Money(amountMinor, currency);
    }

    public static Money zero(String currency) {
        return new Money(0L, currency);
    }

    public long amountMinor() { return amountMinor; }
    public String currency() { return currency; }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(this.amountMinor, other.amountMinor), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(this.amountMinor, other.amountMinor), currency);
    }

    public Money plus(long minor) {
        return new Money(Math.addExact(this.amountMinor, minor), currency);
    }

    public Money minus(long minor) {
        return new Money(Math.subtractExact(this.amountMinor, minor), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(this.amountMinor), currency);
    }

    /**
     * Applies a rate in basis points (1 bps = 0.01%) using half-up rounding.
     * e.g. {@code percentageBps(1000)} = 10% of the amount.
     */
    public Money percentageBps(int bps) {
        long numerator = this.amountMinor * (long) bps;
        long rounded = Math.floorDiv(numerator + (numerator >= 0 ? 5000 : -5000), 10000);
        return new Money(rounded, currency);
    }

    public boolean isNegative() { return amountMinor < 0; }
    public boolean isPositive() { return amountMinor > 0; }
    public boolean isZero() { return amountMinor == 0; }

    public boolean isGreaterThanOrEqual(long minor) { return amountMinor >= minor; }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amountMinor == money.amountMinor && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountMinor, currency);
    }

    @Override
    public String toString() {
        return currency + " " + (amountMinor / 100.0);
    }
}
