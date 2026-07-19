package com.paymentprocessor.paymentservice.connector;

/** Non-sensitive instrument metadata returned by the Vault for receipts. */
public record InstrumentDetails(String maskedPan, String network, String expiryMonth, String expiryYear) {
}
