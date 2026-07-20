package com.paymentprocessor.disputeservice.domain.enums;

/**
 * The binding decision rendered by the network at arbitration.
 */
public enum ArbitrationOutcome {

    /** Decided in the merchant's favour. */
    WON,

    /** Decided against the merchant. */
    LOST
}
