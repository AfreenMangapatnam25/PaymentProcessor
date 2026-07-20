package com.paymentprocessor.merchantservice.common.enums;

/**
 * Event types a merchant may subscribe a webhook endpoint to.
 * {@link #wireName} is the dot-notation identifier used on the wire.
 */
public enum WebhookEventType {
    PAYMENT_SUCCEEDED("payment.succeeded"),
    PAYMENT_FAILED("payment.failed"),
    REFUND_PROCESSED("refund.processed"),
    CHARGEBACK_RECEIVED("chargeback.received"),
    PAYOUT_COMPLETED("payout.completed"),
    MERCHANT_STATUS_CHANGED("merchant.status_changed");

    private final String wireName;

    WebhookEventType(String wireName) {
        this.wireName = wireName;
    }

    public String getWireName() {
        return wireName;
    }
}
