package com.paymentprocessor.disputeservice.domain.enums;

/**
 * Review lifecycle of a single piece of evidence.
 */
public enum EvidenceStatus {

    /** Uploaded by the merchant, awaiting malware scan / review. */
    UPLOADED,

    /** Passed scanning, being reviewed by the dispute team. */
    UNDER_REVIEW,

    /** Accepted into the representment package. */
    ACCEPTED,

    /** Rejected as insufficient, illegible or irrelevant. */
    REJECTED,

    /** Submitted to the network as part of a representment. */
    SUBMITTED
}
