package com.paymentprocessor.settlementservice.enums;

/**
 * Supported banking / payment rails together with their default per-transfer
 * fee (minor units) and whether they settle in real time.
 *
 * <p>A {@code null} currency means the rail supports any currency (cross-border).
 */
public enum Rail {
    ACH("USD", 100, false),
    RTP("USD", 50, true),
    WIRE("USD", 2500, false),
    SEPA("EUR", 50, false),
    SEPA_INSTANT("EUR", 50, true),
    FPS("GBP", 30, true),
    BACS("GBP", 20, false),
    CHAPS("GBP", 2500, false),
    SWIFT(null, 3500, false);

    private final String currency;
    private final long defaultFeeMinor;
    private final boolean instant;

    Rail(String currency, long defaultFeeMinor, boolean instant) {
        this.currency = currency;
        this.defaultFeeMinor = defaultFeeMinor;
        this.instant = instant;
    }

    public String getCurrency() { return currency; }
    public long getDefaultFeeMinor() { return defaultFeeMinor; }
    public boolean isInstant() { return instant; }

    /** True if this rail can carry the given ISO currency code. */
    public boolean supportsCurrency(String iso) {
        return currency == null || currency.equalsIgnoreCase(iso);
    }
}
