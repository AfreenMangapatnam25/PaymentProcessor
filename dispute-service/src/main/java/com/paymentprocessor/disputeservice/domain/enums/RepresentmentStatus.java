package com.paymentprocessor.disputeservice.domain.enums;

/**
 * Status of a representment submission to the card network.
 */
public enum RepresentmentStatus {

    /** Being assembled, not yet submitted. */
    DRAFT,

    /** Sent to the acquirer / network, awaiting issuer decision. */
    SUBMITTED,

    /** Issuer accepted the representment; dispute won. */
    ACCEPTED,

    /** Issuer rejected the representment; may proceed to pre-arbitration. */
    REJECTED
}
