package com.paymentprocessor.disputeservice.domain.enums;

/**
 * The phase of the dispute within the overall chargeback flow. A dispute can
 * move through successive stages, each with its own network deadline.
 */
public enum DisputeStage {

    /** Issuer requested transaction documentation before a formal chargeback. */
    RETRIEVAL,

    /** First chargeback raised by the issuer. */
    CHARGEBACK,

    /** Merchant challenge submitted to the network. */
    REPRESENTMENT,

    /** Issuer rejected the representment and issued a pre-arbitration notice. */
    PRE_ARBITRATION,

    /** Case escalated to the network for a binding decision. */
    ARBITRATION
}
