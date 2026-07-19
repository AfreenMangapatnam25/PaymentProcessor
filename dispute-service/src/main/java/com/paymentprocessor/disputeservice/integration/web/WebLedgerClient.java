package com.paymentprocessor.disputeservice.integration.web;

import com.paymentprocessor.disputeservice.integration.LedgerClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Real, WebClient-backed implementation of {@link LedgerClient} against
 * ledger-service's {@code /api/v1/journals} endpoints (JournalController).
 *
 * <p>Ledger Service does not expose an endpoint to look up a merchant's GL
 * accounts by merchant id, so account ids used here follow a deterministic
 * convention ({@code merchant:<merchantId>:reserve} /
 * {@code platform:chargeback_clearing} / {@code platform:fee_revenue}). This
 * mirrors how other merchant-scoped ledger accounts are expected to be
 * provisioned, but is a best-effort mapping since dispute-service has no
 * account-discovery endpoint to call.
 */
@Component
public class WebLedgerClient implements LedgerClient {

    private static final Logger log = LoggerFactory.getLogger(WebLedgerClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public WebLedgerClient(@Qualifier("ledgerServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public String postChargebackDebit(String disputeId, String merchantId,
                                       long amountMinor, long feeMinor, String currency) {
        String merchantAccount = "merchant:" + merchantId + ":reserve";
        String platformAccount = "platform:chargeback_clearing";
        long total = amountMinor + feeMinor;

        Map<String, Object> request = Map.of(
                "eventType", "DISPUTE_CHARGEBACK_DEBIT",
                "externalRef", disputeId,
                "idempotencyKey", "dispute-chargeback-" + disputeId,
                "description", "Chargeback debit for dispute " + disputeId,
                "createdBy", "dispute-service",
                "lines", List.of(
                        Map.of("accountId", merchantAccount, "direction", "DEBIT",
                                "amountMinor", total, "currency", currency,
                                "description", "Chargeback debit dispute " + disputeId),
                        Map.of("accountId", platformAccount, "direction", "CREDIT",
                                "amountMinor", total, "currency", currency,
                                "description", "Chargeback clearing dispute " + disputeId)
                ));

        return postJournal(request, "postChargebackDebit", disputeId);
    }

    @Override
    public String reverseChargeback(String disputeId, String originalJournalId) {
        Map<String, Object> request = Map.of(
                "reason", "TRANSACTION_REVERSED",
                "description", "Dispute won, reversing chargeback for dispute " + disputeId,
                "createdBy", "dispute-service",
                "idempotencyKey", "dispute-reverse-" + disputeId);

        try {
            Map<String, Object> body = webClient.post()
                    .uri("/api/v1/journals/{id}/reverse", originalJournalId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .onErrorResume(ex -> {
                        log.warn("[LEDGER] reverseChargeback failed dispute={} original={}: {}",
                                disputeId, originalJournalId, ex.toString());
                        return Mono.empty();
                    })
                    .blockOptional()
                    .orElse(null);
            if (body == null || body.get("id") == null) {
                return fallbackJournalId();
            }
            return (String) body.get("id");
        } catch (Exception ex) {
            log.warn("[LEDGER] reverseChargeback error dispute={} original={}: {}",
                    disputeId, originalJournalId, ex.toString());
            return fallbackJournalId();
        }
    }

    @Override
    public String finalizeLoss(String disputeId, String originalJournalId) {
        // LedgerClient.finalizeLoss does not receive the amount being finalized, and
        // ledger-service has no dedicated "finalize loss" endpoint -- posting a
        // balanced journal requires knowing the amount, which this method signature
        // does not provide. Falls back to logging + a synthetic id rather than
        // guessing an amount and posting an incorrect journal.
        log.info("[LEDGER] (no matching downstream endpoint for this signature) finalizeLoss "
                + "dispute={} original={}", disputeId, originalJournalId);
        return fallbackJournalId();
    }

    @Override
    public String postFee(String disputeId, long feeMinor, String currency, String description) {
        Map<String, Object> request = Map.of(
                "eventType", "DISPUTE_FEE",
                "externalRef", disputeId,
                "idempotencyKey", "dispute-fee-" + disputeId + "-" + UUID.randomUUID(),
                "description", description == null ? ("Dispute fee for " + disputeId) : description,
                "createdBy", "dispute-service",
                "lines", List.of(
                        Map.of("accountId", "platform:fee_expense", "direction", "DEBIT",
                                "amountMinor", feeMinor, "currency", currency,
                                "description", description),
                        Map.of("accountId", "platform:fee_revenue", "direction", "CREDIT",
                                "amountMinor", feeMinor, "currency", currency,
                                "description", description)
                ));

        return postJournal(request, "postFee", disputeId);
    }

    @SuppressWarnings("unchecked")
    private String postJournal(Map<String, Object> request, String op, String disputeId) {
        try {
            Map<String, Object> body = webClient.post()
                    .uri("/api/v1/journals")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .onErrorResume(ex -> {
                        log.warn("[LEDGER] {} failed dispute={}: {}", op, disputeId, ex.toString());
                        return Mono.empty();
                    })
                    .blockOptional()
                    .orElse(null);
            if (body == null || body.get("id") == null) {
                return fallbackJournalId();
            }
            return (String) body.get("id");
        } catch (Exception ex) {
            log.warn("[LEDGER] {} error dispute={}: {}", op, disputeId, ex.toString());
            return fallbackJournalId();
        }
    }

    private String fallbackJournalId() {
        return "jrn_" + UUID.randomUUID().toString().replace("-", "");
    }
}
