package com.paymentprocessor.authorization.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Instruction to capture a previously authorized hold. A null {@code amount} captures the full
 * authorized amount.
 */
@Value
@Builder
public class GatewayCaptureCommand {
    String gatewayAuthorizationId;
    BigDecimal amount;
    String currency;
    String idempotencyKey;
}
