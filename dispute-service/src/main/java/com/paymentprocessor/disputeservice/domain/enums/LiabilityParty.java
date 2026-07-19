package com.paymentprocessor.disputeservice.domain.enums;

/**
 * Which party currently bears the financial liability for a dispute.
 */
public enum LiabilityParty {

    /** Liability rests with the merchant (default until resolved in their favour). */
    MERCHANT,

    /** Liability absorbed by the platform. */
    PLATFORM,

    /** Not yet determined; the dispute is still in flight. */
    PENDING
}
