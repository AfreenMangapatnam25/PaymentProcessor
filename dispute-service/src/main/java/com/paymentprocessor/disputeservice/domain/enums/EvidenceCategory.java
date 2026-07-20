package com.paymentprocessor.disputeservice.domain.enums;

/**
 * Semantic categorisation of an uploaded evidence document, used to check that
 * a package covers the evidence a given reason code requires.
 */
public enum EvidenceCategory {
    RECEIPT,
    INVOICE,
    TRACKING,
    DELIVERY_CONFIRMATION,
    SIGNATURE_PROOF,
    COMMUNICATION,
    REFUND_POLICY,
    REFUND_PROOF,
    AUTHORIZATION_RECORD,
    AVS_CVV_RESULT,
    THREE_DS_PROOF,
    DEVICE_FINGERPRINT,
    TERMS_OF_SERVICE,
    PRODUCT_DESCRIPTION,
    CANCELLATION_PROOF,
    OTHER
}
