package com.paymentprocessor.disputeservice.domain.enums;

/**
 * The origin of a dispute notification.
 */
public enum DisputeSource {

    /** Directly from a card network (Visa, Mastercard, etc.). */
    CARD_NETWORK,

    /** Forwarded by the acquiring bank. */
    ACQUIRER,

    /** Payment gateway dispute webhook (e.g. Stripe dispute.created). */
    PAYMENT_GATEWAY,

    /** Manually created by the operations team for non-standard scenarios. */
    INTERNAL
}
