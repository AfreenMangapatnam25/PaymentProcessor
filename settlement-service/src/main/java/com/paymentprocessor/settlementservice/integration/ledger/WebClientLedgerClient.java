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
 * <p>The Ledger Service only accepts postings against accounts that already exist
 * (created via {@code POST /api/v1/accounts}); it has no merchant-aware account
 * lookup. This client therefore addresses accounts by a deterministic naming
 * convention â€” {@code merchant:<merchantId>:settlement_liability},
 * {@code merchant:<merchantId>:reserve}, and a small set of platform-owned control
 * accounts (cash, fee revenue, payout payable, adjustment expense). Those accounts
 * are expected to be provisioned ahead of time (onboarding / seed data); this
 * client does not create them on the fly.
 *
 * <p>Every posting here is a balanced journal (debits == credits), which the
 * Ledger Service enforces server-side via {@code UnbalancedJournalException}.
 */
@Component
@Primary
public class WebClientLedgerClient implements LedgerClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientLedgerClient.class);

    private static final String PLATFORM_CASH = "platform:cash";
    private static final String PLATFORM_FEE_REVENUE = "platform:fee_revenue";
    private static final String PLATFORM_PAYOUT_PAYABLE = "platform:payout_payable";
    private static final String PLATFORM_ADJUSTMENT_EXPENSE = "platform:adjustment_expense";

    private final WebClient ledgerWebClient;
    private final Duration timeout;

    public WebClientLedgerClient(WebClient ledgerWebClient,
                                  @Value("${settlement.integration.ledger.timeout-ms:5000}") long timeoutMs) {
        this.ledgerWebClient = ledgerWebClient;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public String postSettlement(String batchId, String merchantId, String currency,
                                 long netMinor, long feesMinor, long reserveMinor) {
        long gross = netMinor + feesMinor + reserveMinor;
        return post("SETTLEMENT", batchId, currency, List.of(
                line(liabilityAccount(merchantId), "DEBIT", gross, currency),
                line(PLATFORM_FEE_REVENUE, "CREDIT", feesMinor, currency),
                line(reserveAccount(merchantId), "CREDIT", reserveMinor, currency),
                line(PLATFORM_PAYOUT_PAYABLE, "CREDIT", netMinor, currency)
        ).stream().filter(l -> (Long) l.get("amountMinor") > 0).toList(),
                "Settlement batch " + batchId + " for merchant " + merchantId);
    }

    @Override
    public String postReserveHold(String reserveId, String merchantId, String currency, long amountMinor) {
        return post("RESERVE_HOLD", reserveId, currency, List.of(
                line(liabilityAccount(merchantId), "DEBIT", amountMinor, currency),
                line(reserveAccount(merchantId), "CREDIT", amountMinor, currency)
        ), "Reserve hold " + reserveId + " for merchant " + merchantId);
    }

    @Override
    public String postReserveRelease(String reserveId, String merchantId, String currency, long amountMinor) {
        return post("RESERVE_RELEASE", reserveId, currency, List.of(
                line(reserveAccount(merchantId), "DEBIT", amountMinor, currency),
                line(liabilityAccount(merchantId), "CREDIT", amountMinor, currency)
        ), "Reserve release " + reserveId + " for merchant " + merchantId);
    }

    @Override
    public String postPayout(String payoutId, String merchantId, String currency, long amountMinor) {
        return post("PAYOUT", payoutId, currency, List.of(
                line(PLATFORM_PAYOUT_PAYABLE, "DEBIT", amountMinor, currency),
                line(PLATFORM_CASH, "CREDIT", amountMinor, currency)
        ), "Payout " + payoutId + " for merchant " + merchantId);
    }

    @Override
    public String postPayoutReturn(String payoutId, String merchantId, String currency, long amountMinor) {
        return post("PAYOUT_RETURN", payoutId, currency, List.of(
                line(PLATFORM_CASH, "DEBIT", amountMinor, currency),
                line(PLATFORM_PAYOUT_PAYABLE, "CREDIT", amountMinor, currency)
        ), "Payout return " + payoutId + " for merchant " + merchantId);
    }

    @Override
    public String postReversal(String batchId, String merchantId, String currency, long amountMinor, String reason) {
        return post("REVERSAL", batchId, currency, List.of(
                line(liabilityAccount(merchantId), "DEBIT", amountMinor, currency),
                line(PLATFORM_CASH, "CREDIT", amountMinor, currency)
        ), "Settlement reversal for batch " + batchId + " (" + reason + ")");
    }

    @Override
    public String postAdjustment(String adjustmentId, String merchantId, String currency, long signedAmountMinor) {
        long amount = Math.abs(signedAmountMinor);
        List<Map<String, Object>> lines = signedAmountMinor >= 0
                ? List.of(
                    line(PLATFORM_ADJUSTMENT_EXPENSE, "DEBIT", amount, currency),
                    line(liabilityAccount(merchantId), "CREDIT", amount, currency))
                : List.of(
                    line(liabilityAccount(merchantId), "DEBIT", amount, currency),
                    line(PLATFORM_ADJUSTMENT_EXPENSE, "CREDIT", amount, currency));
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

    private static String liabilityAccount(String merchantId) {
        return "merchant:" + merchantId + ":settlement_liability";
    }

    private static String reserveAccount(String merchantId) {
        return "merchant:" + merchantId + ":reserve";
    }
}
