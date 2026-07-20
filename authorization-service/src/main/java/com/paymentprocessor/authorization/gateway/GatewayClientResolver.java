package com.paymentprocessor.authorization.gateway;

import com.paymentprocessor.authorization.config.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selects the active {@link GatewayClient} based on {@code gateway.provider}. Multiple gateway
 * implementations can coexist; the configured provider is resolved on startup.
 */
@Slf4j
@Component
public class GatewayClientResolver {

    private final Map<String, GatewayClient> clientsByProvider;
    private final GatewayClient active;

    public GatewayClientResolver(List<GatewayClient> clients, GatewayProperties properties) {
        this.clientsByProvider = clients.stream()
                .collect(Collectors.toMap(c -> c.provider().toLowerCase(), Function.identity()));
        String provider = properties.getProvider() == null ? "" : properties.getProvider().toLowerCase();
        this.active = clientsByProvider.get(provider);
        if (this.active == null) {
            throw new IllegalStateException("No GatewayClient found for provider '" + provider
                    + "'. Available: " + clientsByProvider.keySet());
        }
        log.info("Active payment gateway provider: {}", provider);
    }

    /** The gateway client selected by configuration. */
    public GatewayClient active() {
        return active;
    }

    /** Look up a specific provider (e.g. for multi-acquirer routing). */
    public GatewayClient forProvider(String provider) {
        GatewayClient client = clientsByProvider.get(provider == null ? "" : provider.toLowerCase());
        if (client == null) {
            throw new IllegalArgumentException("Unknown gateway provider: " + provider);
        }
        return client;
    }
}
