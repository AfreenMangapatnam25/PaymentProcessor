package com.paymentprocessor.settlementservice.integration.ledger;

import com.paymentprocessor.settlementservice.exception.LedgerServiceException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Posts real double-entry journals to the Ledger Service ({@code POST /api/v1/journals}).
 *
 * <p>Account ids are resolved dynamically via {@link LedgerAccountResolver}, which calls
 * ledger-service's {@code GET /api/v1/accounts/resolve} and
 * {@code POST /api/v1/accounts/provision-merchant} endpoints.
 */
@Component
@Primary
public class WebClientLedgerClient implements LedgerClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientLedgerClient.class);

    private final WebClient ledgerWebClient;
    private final LedgerAccountResolver accountResolver;
    private final Duration timeout;

    public WebClientLedgerClient(WebClient ledgerWebClient,
                                 LedgerAccountResolver accountResolver,
                                 @Value("${settlement.integration.ledger.timeout-ms:5000}") long timeoutMs) {
        this.ledgerWebClient = ledgerWebClient;
        this.accountResolver = accountResolver;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public String postSettlement(String batchId, String merchantId, String currency,
                                 long netMinor, long feesMinor, long reserveMinor) {
        long gross = netMinor + feesMinor + reserveMinor;
        return post("SETTLEMENT", batchId, currency, List.of(
                line(accountResolver.merchantSettlementLiability(merchantId, currency), "DEBIT", gross, currency),
                line(accountResolver.platformFeeRevenue(), "CREDIT", feesMinor, currency),
                line(accountResolver.merchantReserve(merchantId, currency), "CREDIT", reserveMinor, currency),
                line(accountResolver.platformPayoutPayable(), "CREDIT", netMinor, currency)
        ).stream().filter(l -> (Long) l.get("amountMinor") > 0).toList(),
                "Settlement batch " + batchId + " for merchant " + merchantId);
    }

    @Override
    public String postReserveHold(String reserveId, String merchantId, String currency, long amountMinor) {
        return post("RESERVE_HOLD", reserveId, currency, List.of(
                line(accountResolver.merchantSettlementLiability(merchantId, currency), "DEBIT", amountMinor, currency),
                line(accountResolver.merchantReserve(merchantId, currency), "CREDIT", amountMinor, currency)
        ), "Reserve hold " + reserveId + " for merchant " + merchantId);
    }

    @Override
    public String postReserveRelease(String reserveId, String merchantId, String currency, long amountMinor) {
        return post("RESERVE_RELEASE", reserveId, currency, List.of(
                line(accountResolver.merchantReserve(merchantId, currency), "DEBIT", amountMinor, currency),
                line(accountResolver.merchantSettlementLiability(merchantId, currency), "CREDIT", amountMinor, currency)
        ), "Reserve release " + reserveId + " for merchant " + merchantId);
    }

    @Override
    public String postPayout(String payoutId, String merchantId, String currency, long amountMinor) {
        return post("PAYOUT", payoutId, currency, List.of(
                line(accountResolver.platformPayoutPayable(), "DEBIT", amountMinor, currency),
                line(accountResolver.platformCash(), "CREDIT", amountMinor, currency)
        ), "Payout " + payoutId + " for merchant " + merchantId);
    }

    @Override
    public String postPayoutReturn(String payoutId, String merchantId, String currency, long amountMinor) {
        return post("PAYOUT_RETURN", payoutId, currency, List.of(
                line(accountResolver.platformCash(), "DEBIT", amountMinor, currency),
                line(accountResolver.platformPayoutPayable(), "CREDIT", amountMinor, currency)
        ), "Payout return " + payoutId + " for merchant " + merchantId);
    }

    @Override
    public String postReversal(String batchId, String merchantId, String currency, long amountMinor, String reason) {
        return post("REVERSAL", batchId, currency, List.of(
                line(accountResolver.merchantSettlementLiability(merchantId, currency), "DEBIT", amountMinor, currency),
                line(accountResolver.platformCash(), "CREDIT", amountMinor, currency)
        ), "Settlement reversal for batch " + batchId + " (" + reason + ")");
    }

    @Override
    public String postAdjustment(String adjustmentId, String merchantId, String currency, long signedAmountMinor) {
        long amount = Math.abs(signedAmountMinor);
        List<Map<String, Object>> lines = signedAmountMinor >= 0
                ? List.of(
                    line(accountResolver.platformAdjustmentExpense(), "DEBIT", amount, currency),
                    line(accountResolver.merchantSettlementLiability(merchantId, currency), "CREDIT", amount, currency))
                : List.of(
                    line(accountResolver.merchantSettlementLiability(merchantId, currency), "DEBIT", amount, currency),
                    line(accountResolver.platformAdjustmentExpense(), "CREDIT", amount, currency));
        return post("ADJUSTMENT", adjustmentId, currency, lines, "Adjustment " + adjustmentId + " for merchant " + merchantId);
    }

    private String post(String eventType, String externalRef, String currency,
                        List<Map<String, Object>> lines, String description) {
        Map<String, Object> request = Map.of(
                "eventType", eventType,
                "externalRef", externalRef,
                "idempotencyKey", eventType + ":" + externalRef,
                "description", description,
                "lines", lines
        );
        try {
            Map<?, ?> response = ledgerWebClient.post()
                    .uri("/api/v1/journals")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .block();
            Object journalId = response != null ? response.get("id") : null;
            if (journalId == null) {
                throw new LedgerServiceException("Ledger Service returned no journal id for " + eventType + " " + externalRef);
            }
            return journalId.toString();
        } catch (WebClientResponseException e) {
            log.error("Ledger posting [{}] {} failed: {} {}", eventType, externalRef, e.getStatusCode(), e.getResponseBodyAsString());
            throw new LedgerServiceException("Ledger Service rejected " + eventType + " posting for " + externalRef, e);
        } catch (LedgerServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ledger posting [{}] {} failed", eventType, externalRef, e);
            throw new LedgerServiceException("Ledger Service unreachable for " + eventType + " posting " + externalRef, e);
        }
    }

    private static Map<String, Object> line(String accountId, String direction, long amountMinor, String currency) {
        return Map.of(
                "accountId", accountId,
                "direction", direction,
                "amountMinor", amountMinor,
                "currency", currency
        );
    }
}
