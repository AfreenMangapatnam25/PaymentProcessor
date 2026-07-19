package com.paymentprocessor.limit.domain.enums;

/**
 * What a limit constrains.
 * AMOUNT — total monetary value within the window.
 * COUNT  — number of transactions within the window.
 */
public enum LimitDimension {
    AMOUNT,
    COUNT
}
