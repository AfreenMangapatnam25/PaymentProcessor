package com.paymentprocessor.disputeservice.domain.enums;

/**
 * The nature of the inbound dispute notification.
 */
public enum DisputeType {

    /** Customer disputed a transaction with their issuing bank. */
    CHARGEBACK,

    /** Issuer requests transaction documentation before a formal chargeback. */
    RETRIEVAL_REQUEST,

    /** Issuer escalated after rejecting a representment. */
    PRE_ARBITRATION
}
