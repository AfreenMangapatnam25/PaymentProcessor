package com.paymentprocessor.settlementservice.integration.rail;

/**
 * Acknowledgement returned by a rail after a transfer is submitted.
 *
 * @param providerRef the rail's tracking reference
 * @param processing  true if the rail acknowledged and is asynchronously
 *                    processing; false if it settled synchronously (instant rails)
 */
public record RailAck(String providerRef, boolean processing) {
}
