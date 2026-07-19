package com.paymentprocessor.settlementservice.integration.merchant;

import com.paymentprocessor.settlementservice.enums.ScheduleType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Reads merchant settlement configuration from the real Merchant Service:
 * merchant status/currency via {@code GET /api/v1/merchants/{id}} and the
 * default settlement (payout) bank account via
 * {@code GET /api/v1/merchants/{id}/settlement-accounts}.
 *
 * <p>Fee/reserve rate configuration (platform fee bps, reserve rate/hold days)
 * is not yet exposed by a dedicated Merchant Service endpoint reachable from
 * here, so those fields fall back to the platform default schedule. Swap in a
 * real fee-schedule lookup once merchant-service exposes one.
 */
@Component
@Primary
public class WebClientMerchantClient implements MerchantClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientMerchantClient.class);

    private final WebClient merchantWebClient;
    private final Duration timeout;

    public WebClientMerchantClient(WebClient merchantWebClient,
                                   @Value("${settlement.integration.merchant.timeout-ms:3000}") long timeoutMs) {
        this.merchantWebClient = merchantWebClient;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public Optional<MerchantSettlementProfile> getProfile(String merchantId) {
        try {
            Map<?, ?> merchant = merchantWebClient.get()
                    .uri("/api/v1/merchants/{id}", merchantId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, resp -> Mono.empty())
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .block();
            if (merchant == null) {
                return Optional.empty();
            }
            boolean active = "ACTIVE".equals(String.valueOf(merchant.get("status")));
            String currency = merchant.get("defaultCurrency") != null
                    ? merchant.get("defaultCurrency").toString() : "USD";

            List<Map<String, Object>> accounts = merchantWebClient.get()
                    .uri("/api/v1/merchants/{id}/settlement-accounts", merchantId)
                    .retrieve()
                    .bodyToMono(List.class)
                    .timeout(timeout)
                    .onErrorReturn(List.of())
                    .block();
            String payoutAccountId = accounts == null ? null : accounts.stream()
                    .filter(a -> Boolean.TRUE.equals(a.get("defaultAccount")))
                    .findFirst()
                    .map(a -> String.valueOf(a.get("id")))
                    .orElse(null);

            return Optional.of(new MerchantSettlementProfile(
                    merchantId,
                    active,
                    currency,
                    ScheduleType.DAILY,
                    payoutAccountId,
                    290,
                    30,
                    1000,
                    90,
                    true,
                    null,
                    false
            ));
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Merchant Service lookup failed for merchant {}", merchantId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean isPayoutAccountValid(String merchantId, String payoutAccountId) {
        if (payoutAccountId == null || payoutAccountId.isBlank()) {
            return false;
        }
        try {
            List<Map<String, Object>> accounts = merchantWebClient.get()
                    .uri("/api/v1/merchants/{id}/settlement-accounts", merchantId)
                    .retrieve()
                    .bodyToMono(List.class)
                    .timeout(timeout)
                    .block();
            if (accounts == null) {
                return false;
            }
            return accounts.stream()
                    .filter(a -> payoutAccountId.equals(String.valueOf(a.get("id"))))
                    .anyMatch(a -> "VERIFIED".equals(String.valueOf(a.get("verificationStatus"))));
        } catch (Exception e) {
            log.error("Merchant Service account-validity check failed for merchant {} account {}",
                    merchantId, payoutAccountId, e);
            return false;
        }
    }
}
