package com.paymentprocessor.userservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code app.*} configuration tree. Immutable
 * (records + constructor binding) so configuration cannot drift at runtime.
 * Registered via {@code @ConfigurationPropertiesScan} on the application class.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Security security,
        Crypto crypto,
        Outbox outbox
) {

    public record Security(Jwt jwt) {
        public record Jwt(
                String authoritiesClaim,
                String authoritiesPrefix,
                String merchantIdClaim,
                String identityIdClaim
        ) {
        }
    }

    public record Crypto(Kms kms, DataKey dataKey, BlindIndex blindIndex) {

        public record Kms(String provider, String masterKeyId) {
        }

        public record DataKey(
                String algorithm,
                int keySizeBits,
                String cipherTransformation,
                int gcmTagLengthBits
        ) {
        }

        public record BlindIndex(String hmacAlgorithm, String keyId) {
        }
    }

    public record Outbox(Relay relay) {
        public record Relay(
                boolean enabled,
                long pollIntervalMs,
                int batchSize,
                int maxAttempts
        ) {
        }
    }
}
