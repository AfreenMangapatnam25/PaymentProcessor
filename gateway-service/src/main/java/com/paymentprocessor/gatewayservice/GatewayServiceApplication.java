package com.paymentprocessor.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * API Gateway — the single ingress edge for the payment platform.
 *
 * <p>Terminates TLS, authenticates every request by validating short-lived JWTs
 * (or API keys) locally against a cached JWKS, enforces per-key rate limits backed
 * by Redis, applies resilience patterns, and routes to downstream services. It owns
 * nothing durable: if it loses Redis it degrades, it does not lose data.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
