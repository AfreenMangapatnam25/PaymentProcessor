package com.paymentprocessor.authorization.gateway.model;

import lombok.Builder;
import lombok.Value;

/**
 * Instruction to reverse (void) an authorization hold before capture.
 */
@Value
@Builder
public class GatewayReversalCommand {
    String gatewayAuthorizationId;
    String reason;
    String idempotencyKey;
}
