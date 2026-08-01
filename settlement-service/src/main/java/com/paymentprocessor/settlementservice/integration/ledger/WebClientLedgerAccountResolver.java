package com.paymentprocessor.settlementservice.integration.ledger;

import com.paymentprocessor.settlementservice.exception.LedgerServiceException;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Resolves ledger account ids dynamically from ledger-service's
 * {@code GET /api/v1/accounts/resolve} endpoint instead of hard-coding naming
 * conventions locally.
 */
@Component
@Primary
public class WebClientLedgerAccountResolver implements LedgerAccountResolver {

    private static final Logger log = LoggerFactory.getLogger(WebClientLedgerAccountResolver.class);

    private final WebClient ledgerWebClient;
    private final Duration timeout;

    public WebClientLedgerAccountResolver(WebClient ledgerWebClient,
                                          @Value("${settlement.integration.ledger.timeout-ms:5000}") long timeoutMs) {
        this.ledgerWebClient = ledgerWebClient;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public String platformCash() {
        return resolve("PLATFORM_CASH", null, "USD");
    }

    @Override
    public String platformFeeRevenue() {
        return resolve("PLATFORM_FEE_REVENUE", null, "USD");
    }

    @Override
    public String platformPayoutPayable() {
        return resolve("PLATFORM_PAYOUT_PAYABLE", null, "USD");
    }

    @Override
    public String platformAdjustmentExpense() {
        return resolve("PLATFORM_ADJUSTMENT_EXPENSE", null, "USD");
    }

    @Override
    public String merchantSettlementLiability(String merchantId, String currency) {
        return resolve("MERCHANT_SETTLEMENT_LIABILITY", merchantId, currency);
    }

    @Override
    public String merchantReserve(String merchantId, String currency) {
        return resolve("MERCHANT_RESERVE", merchantId, currency);
    }

    private String resolve(String purpose, String ownerId, String currency) {
        try {
            Map<?, ?> response = ledgerWebClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/v1/accounts/resolve")
                                .queryParam("purpose", purpose)
                                .queryParam("currency", currency);
                        if (ownerId != null) {
                            builder.queryParam("ownerId", ownerId);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .block();
            if (response == null || response.get("id") == null) {
                throw new LedgerServiceException("Ledger Service returned no account id for purpose " + purpose);
            }
            return response.get("id").toString();
        } catch (WebClientResponseException e) {
            log.error("Ledger account resolve [{}] failed: {} {}", purpose, e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new LedgerServiceException("Ledger Service rejected account resolve for " + purpose, e);
        } catch (LedgerServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ledger account resolve [{}] failed", purpose, e);
            throw new LedgerServiceException("Ledger Service unreachable for account resolve " + purpose, e);
        }
    }
}
