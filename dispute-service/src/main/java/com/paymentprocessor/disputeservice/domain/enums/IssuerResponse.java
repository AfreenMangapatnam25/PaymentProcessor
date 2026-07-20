package com.paymentprocessor.disputeservice.domain.enums;

/**
 * The issuer's response to a submitted representment.
 */
public enum IssuerResponse {

    /** Issuer accepted the merchant's evidence; chargeback reversed. */
    ACCEPTED,

    /** Issuer rejected the representment; chargeback stands. */
    REJECTED,

    /** Issuer escalated the case to pre-arbitration. */
    ESCALATED
}
