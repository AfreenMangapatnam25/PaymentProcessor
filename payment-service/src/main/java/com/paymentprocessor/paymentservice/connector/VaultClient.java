package com.paymentprocessor.paymentservice.connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.paymentprocessor.paymentservice.config.ConnectorProperties;
import com.paymentprocessor.paymentservice.config.HttpClientConfig.RestClientFactory;

/**
 * Thin client over the Tokenization / Vault service. Used only to fetch masked,
 * non-sensitive instrument metadata (e.g. last-4, network) for receipts. The
 * payment service never handles raw PAN/CVV. Returns {@code null} when disabled
 * or unavailable.
 */
@Component
public class VaultClient {

    private static final Logger log = LoggerFactory.getLogger(VaultClient.class);

    private final RestClient client;
    private final boolean enabled;

    public VaultClient(ConnectorProperties props, RestClientFactory factory) {
        this.client = factory.build(props.getVault());
        this.enabled = props.getVault().isEnabled();
    }

    public InstrumentDetails detokenize(String instrumentToken) {
        if (!enabled || instrumentToken == null || instrumentToken.isBlank()) {
            return null;
        }
        try {
            VaultResponse r = client.get().uri("/v1/instruments/{token}", instrumentToken)
                    .retrieve().body(VaultResponse.class);
            if (r == null) return null;
            return new InstrumentDetails(r.maskedPan(), r.network(), r.expiryMonth(), r.expiryYear());
        } catch (Exception e) {
            log.warn("Vault detokenize unavailable: {}", e.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VaultResponse(String maskedPan, String network, String expiryMonth, String expiryYear) {
    }
}
