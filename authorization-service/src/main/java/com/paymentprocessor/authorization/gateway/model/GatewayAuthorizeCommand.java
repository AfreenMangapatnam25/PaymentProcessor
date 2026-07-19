package com.paymentprocessor.authorization.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Provider-neutral instruction to place an authorization (funds hold) with the gateway.
 */
@Value
@Builder
public class GatewayAuthorizeCommand {
    String merchantId;
    String paymentReference;
    BigDecimal amount;
    String currency;

    /** Tokenized payment method (e.g. Stripe {@code pm_...}/{@code tok_...}); never a raw PAN. */
    String paymentMethodToken;

    String customerReference;
    String statementDescriptor;
    boolean captureImmediately;
    boolean threeDsEnabled;
    boolean avsRequired;
    String idempotencyKey;

    @Builder.Default
    Map<String, String> metadata = Map.of();
}
