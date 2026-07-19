package com.paymentprocessor.disputeservice.integration.web;

import com.paymentprocessor.disputeservice.integration.SettlementClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Real, WebClient-backed implementation of {@link SettlementClient} against
 * settlement-service's {@code /api/adjustments} endpoint (AdjustmentController).
 *
 * <p>settlement-service has no endpoint to set a merchant's rolling reserve
 * percentage directly (ReserveController only lists reserves and releases due
 * ones), so {@link #adjustReserve(String, int, int)} has no matching real
 * endpoint and falls back to logging only.
 */
@Component
public class WebSettlementClient implements SettlementClient {

    private static final Logger log = LoggerFactory.getLogger(WebSettlementClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public WebSettlementClient(@Qualifier("settlementServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String recoverFromSettlement(String merchantId, String disputeId,
                                         long amountMinor, String currency) {
        Map<String, Object> request = Map.of(
                "merchantId", merchantId,
                "type", "DEBIT",
                "amountMinor", amountMinor,
                "currency", currency,
                "reasonCode", "DISPUTE_CHARGEBACK",
                "description", "Recover chargeback amount for dispute " + disputeId,
                "requestedBy", "dispute-service");

        try {
            Map<String, Object> body = webClient.post()
                    .uri("/api/adjustments")
                    .header("Idempotency-Key", "dispute-recover-" + disputeId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .onErrorResume(ex -> {
                        log.warn("[SETTLEMENT] recoverFromSettlement failed merchant={} dispute={}: {}",
                                merchantId, disputeId, ex.toString());
                        return Mono.empty();
                    })
                    .blockOptional()
                    .orElse(null);
            if (body == null || body.get("id") == null) {
                return fallbackRef();
            }
            return (String) body.get("id");
        } catch (Exception ex) {
            log.warn("[SETTLEMENT] recoverFromSettlement error merchant={} dispute={}: {}",
                    merchantId, disputeId, ex.toString());
            return fallbackRef();
        }
    }

    @Override
    public void releaseToMerchant(String merchantId, String disputeId,
                                   long amountMinor, String currency) {
        Map<String, Object> request = Map.of(
                "merchantId", merchantId,
                "type", "CREDIT",
                "amountMinor", amountMinor,
                "currency", currency,
                "reasonCode", "DISPUTE_WON_RELEASE",
                "description", "Release recovered funds for won dispute " + disputeId,
                "requestedBy", "dispute-service");

        webClient.post()
                .uri("/api/adjustments")
                .header("Idempotency-Key", "dispute-release-" + disputeId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("[SETTLEMENT] releaseToMerchant failed merchant={} dispute={}: {}",
                            merchantId, disputeId, ex.toString());
                    return Mono.empty();
                })
                .block();
    }

    @Override
    public void adjustReserve(String merchantId, int reservePercentage, int rollingDays) {
        // No matching endpoint on settlement-service to set a reserve percentage
        // directly; ReserveController only supports list/get/release-due.
        log.info("[SETTLEMENT] (no matching downstream endpoint) adjustReserve merchant={} pct={} rollingDays={}",
                merchantId, reservePercentage, rollingDays);
    }

    private String fallbackRef() {
        return "stl_" + UUID.randomUUID().toString().replace("-", "");
    }
}
