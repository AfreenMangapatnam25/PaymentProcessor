package com.paymentprocessor.settlementservice.enums;

/** Category of reserve hold applied to a merchant. */
public enum ReserveKind {
    /** A percentage of each settlement held for a rolling window. */
    ROLLING,
    /** A fixed lump sum held up front. */
    FIXED,
    /** An ad-hoc hold applied by the risk team following an investigation. */
    RISK_HOLD
}
