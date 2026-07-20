package com.paymentprocessor.settlementservice.integration.rail;

import com.paymentprocessor.settlementservice.exception.RailException;

/**
 * Abstraction over a banking / payment rail. Implementations translate a
 * {@link RailTransferRequest} into rail-specific instructions (NACHA, ISO 20022,
 * MT103, RTP/FPS API calls, etc.).
 */
public interface RailGateway {

    /**
     * Submits a transfer to the rail.
     *
     * @return an acknowledgement with a provider reference
     * @throws RailException if the rail rejects the transfer; the exception's
     *                       {@code category} drives retry behaviour
     */
    RailAck submit(RailTransferRequest request) throws RailException;
}
