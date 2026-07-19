package com.paymentprocessor.settlementservice.enums;

/**
 * The nature of a settlement line item. The {@code sign} indicates whether the
 * item increases (+1) or decreases (-1) the merchant's net settlement.
 */
public enum SettlementItemType {
    CAPTURE(+1),
    REFUND(-1),
    CHARGEBACK(-1),
    CHARGEBACK_REVERSAL(+1),
    PLATFORM_FEE(-1),
    INTERCHANGE_FEE(-1),
    SETTLEMENT_FEE(-1),
    RESERVE_HOLD(-1),
    RESERVE_RELEASE(+1),
    ADJUSTMENT_CREDIT(+1),
    ADJUSTMENT_DEBIT(-1),
    PRIOR_PERIOD_CORRECTION(-1);

    private final int sign;

    SettlementItemType(int sign) {
        this.sign = sign;
    }

    public int getSign() {
        return sign;
    }
}
