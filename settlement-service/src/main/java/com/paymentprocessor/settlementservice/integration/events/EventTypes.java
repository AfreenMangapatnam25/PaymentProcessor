package com.paymentprocessor.settlementservice.integration.events;

/** Canonical domain event type names published by the Settlement Service. */
public final class EventTypes {

    public static final String SETTLEMENT_INITIATED = "SettlementInitiated";
    public static final String SETTLEMENT_COMPLETED = "SettlementCompleted";
    public static final String SETTLEMENT_FAILED = "SettlementFailed";
    public static final String SETTLEMENT_REVERSED = "SettlementReversed";
    public static final String PAYOUT_RETURNED = "PayoutReturned";
    public static final String RESERVE_RELEASED = "ReserveReleased";

    private EventTypes() {
    }
}
