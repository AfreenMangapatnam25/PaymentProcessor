package com.paymentprocessor.limit.domain.enums;

/**
 * The reset window over which usage accumulates.
 * PER_TRANSACTION is a special window that applies to a single transaction only
 * and never accumulates.
 */
public enum TimeWindow {
    PER_TRANSACTION,
    DAILY,
    WEEKLY,
    MONTHLY,
    LIFETIME
}
