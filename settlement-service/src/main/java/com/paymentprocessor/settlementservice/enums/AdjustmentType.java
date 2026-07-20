package com.paymentprocessor.settlementservice.enums;

/**
 * Manual correction types applied to a settlement. The {@code sign} indicates
 * the direction of the money movement relative to the merchant payout.
 */
public enum AdjustmentType {
    CREDIT(+1),
    DEBIT(-1),
    FEE_CORRECTION(+1),
    RESERVE_RELEASE(+1),
    RESERVE_HOLD(-1),
    CURRENCY_CORRECTION(+1);

    private final int sign;

    AdjustmentType(int sign) {
        this.sign = sign;
    }

    public int getSign() {
        return sign;
    }
}
