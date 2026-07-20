package com.paymentprocessor.authorization.gateway;

import com.paymentprocessor.authorization.gateway.model.GatewayAuthorizeCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayCaptureCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayResult;
import com.paymentprocessor.authorization.gateway.model.GatewayReversalCommand;

/**
 * Abstraction over an external payment gateway / card network. Implementations translate
 * provider-neutral commands into the provider's protocol (ISO 8583, REST SDK, etc.) and normalize
 * responses back into {@link GatewayResult}. Selecting an implementation is driven by
 * {@code gateway.provider}.
 */
public interface GatewayClient {

    /** Provider id this client handles (matched against {@code gateway.provider}). */
    String provider();

    /** Place an authorization hold (or purchase, when {@code captureImmediately} is set). */
    GatewayResult authorize(GatewayAuthorizeCommand command);

    /** Capture a previously authorized hold, in full or partially. */
    GatewayResult capture(GatewayCaptureCommand command);

    /** Reverse (void) an authorization hold before capture. */
    GatewayResult reverse(GatewayReversalCommand command);

    /** Retrieve the current gateway-side state of an authorization (for inquiry / reconciliation). */
    GatewayResult retrieve(String gatewayAuthorizationId);
}
