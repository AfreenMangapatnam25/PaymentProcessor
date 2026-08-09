package com.paymentprocessor.disputeservice.integration.web;

import com.paymentprocessor.disputeservice.integration.MerchantClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Looks up merchant contact details from merchant-service, accepting either a UUID
 * merchant id or an opaque merchant reference (e.g. {@code MERCH-SEED-01}).
 */
@Component
public class WebMerchantClient implements MerchantClient {

    private static final Logger log = LoggerFactory.getLogger(WebMerchantClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient merchantWebClient;
    private final String adminApiKey;

    public WebMerchantClient(@Qualifier("merchantServiceWebClient") WebClient merchantWebClient,
                             @Value("${merchant.service.admin-api-key:}") String adminApiKey) {
        this.merchantWebClient = merchantWebClient;
        this.adminApiKey = adminApiKey;
    }

    @Override
    public Optional<MerchantContact> findContact(String merchantId) {
        try {
            Map<?, ?> merchant = fetchMerchant(merchantId);
            if (merchant == null) {
                return Optional.empty();
            }
            String resolvedId = merchant.get("id") != null ? merchant.get("id").toString() : merchantId;
            String reference = merchant.get("merchantReference") != null
                    ? merchant.get("merchantReference").toString() : merchantId;
            boolean notifyOnChargeback = true;
            try {
                Map<?, ?> config = merchantWebClient.get()
                        .uri("/api/v1/merchants/{id}/configuration", resolvedId)
                        .headers(this::applyAuth)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(TIMEOUT)
                        .block();
                if (config != null && config.get("notifyOnChargeback") != null) {
                    notifyOnChargeback = Boolean.TRUE.equals(config.get("notifyOnChargeback"));
                }
            } catch (Exception ex) {
                log.debug("Could not load merchant configuration for {}: {}", merchantId, ex.toString());
            }
            return Optional.of(new MerchantContact(
                    resolvedId,
                    reference,
                    merchant.get("supportEmail") != null ? merchant.get("supportEmail").toString() : null,
                    merchant.get("supportPhone") != null ? merchant.get("supportPhone").toString() : null,
                    notifyOnChargeback
            ));
        } catch (Exception ex) {
            log.warn("Merchant lookup failed for {}: {}", merchantId, ex.toString());
            return Optional.empty();
        }
    }

    private Map<?, ?> fetchMerchant(String merchantId) {
        if (looksLikeUuid(merchantId)) {
            return merchantWebClient.get()
                    .uri("/api/v1/merchants/{id}", merchantId)
                    .headers(this::applyAuth)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, resp -> Mono.empty())
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();
        }
        try {
            return merchantWebClient.get()
                    .uri("/api/v1/merchants/by-reference/{ref}", merchantId)
                    .headers(this::applyAuth)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        }
    }

    private void applyAuth(org.springframework.http.HttpHeaders headers) {
        if (adminApiKey != null && !adminApiKey.isBlank()) {
            headers.setBearerAuth(adminApiKey);
        }
    }

    private static boolean looksLikeUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
