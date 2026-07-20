package com.paymentprocessor.authorization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code gateway.*} configuration namespace for external payment providers.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    /** Active provider id (e.g. {@code stripe}). Selects the {@code GatewayClient} implementation. */
    private String provider = "stripe";

    private final Stripe stripe = new Stripe();

    @Getter
    @Setter
    public static class Stripe {
        /** Secret API key. Supplied via the STRIPE_API_KEY environment variable in real environments. */
        private String apiKey;
        /** Connection timeout in milliseconds. */
        private int connectTimeoutMs = 10_000;
        /** Read timeout in milliseconds. */
        private int readTimeoutMs = 30_000;
        /** Maximum automatic network retries performed by the Stripe SDK. */
        private int maxNetworkRetries = 2;
    }
}
