package com.paymentprocessor.authorization.gateway;

import com.paymentprocessor.authorization.gateway.model.GatewayAuthorizeCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayCaptureCommand;
import com.paymentprocessor.authorization.gateway.model.GatewayResult;
import com.paymentprocessor.authorization.gateway.model.GatewayReversalCommand;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Wraps gateway calls with resilience policies (retry with exponential backoff on transient
 * failures, and a circuit breaker) configured under {@code resilience4j.*.gateway}. Only
 * {@link GatewayException}s flagged retryable trigger retries; permanent declines are not retried.
 */
@Component
@RequiredArgsConstructor
public class ResilientGateway {

    private final GatewayClientResolver resolver;

    @Retry(name = "gateway")
    @CircuitBreaker(name = "gateway")
    public GatewayResult authorize(GatewayAuthorizeCommand command) {
        return resolver.active().authorize(command);
    }

    @Retry(name = "gateway")
    @CircuitBreaker(name = "gateway")
    public GatewayResult capture(GatewayCaptureCommand command) {
        return resolver.active().capture(command);
    }

    @Retry(name = "gateway")
    @CircuitBreaker(name = "gateway")
    public GatewayResult reverse(GatewayReversalCommand command) {
        return resolver.active().reverse(command);
    }

    @Retry(name = "gateway")
    @CircuitBreaker(name = "gateway")
    public GatewayResult retrieve(String gatewayAuthorizationId) {
        return resolver.active().retrieve(gatewayAuthorizationId);
    }
}
