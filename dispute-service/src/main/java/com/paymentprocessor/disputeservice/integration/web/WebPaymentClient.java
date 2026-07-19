package com.paymentprocessor.disputeservice.integration.web;

import com.paymentprocessor.disputeservice.integration.PaymentClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Real, WebClient-backed implementation of {@link PaymentClient}.
 *
 * <p>Payment Service exposes {@code GET /v1/payments/{id}} which is used to
 * back {@link #getTransactionDetail(String)}. Payment Service does not
 * currently expose an endpoint to flag/clear a "disputed" marker on a
 * transaction, so {@link #flagTransactionDisputed(String, String)} and
 * {@link #clearDisputedFlag(String, String)} fall back to logging only —
 * there is no real downstream endpoint to call.
 */
@Component
public class WebPaymentClient implements PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(WebPaymentClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public WebPaymentClient(@Qualifier("paymentServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public void flagTransactionDisputed(String transactionId, String disputeId) {
        // No matching endpoint exists on payment-service's PaymentController to
        // mark a transaction as disputed; log so the intent is still visible.
        log.info("[PAYMENT] (no downstream endpoint) flag transaction={} disputed dispute={}",
                transactionId, disputeId);
    }

    @Override
    public void clearDisputedFlag(String transactionId, String disputeId) {
        // No matching endpoint exists on payment-service to clear a disputed flag.
        log.info("[PAYMENT] (no downstream endpoint) clear disputed flag transaction={} dispute={}",
                transactionId, disputeId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<TransactionDetail> getTransactionDetail(String transactionId) {
        try {
            Map<String, Object> body = webClient.get()
                    .uri("/v1/payments/{id}", transactionId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .onErrorResume(ex -> {
                        log.warn("[PAYMENT] getTransactionDetail failed for transaction={}: {}",
                                transactionId, ex.toString());
                        return Mono.empty();
                    })
                    .blockOptional()
                    .orElse(null);

            if (body == null) {
                return Optional.empty();
            }

            Long amountMinor = body.get("amountMinor") == null
                    ? null : ((Number) body.get("amountMinor")).longValue();
            String currency = (String) body.get("currency");

            // Payment Service's PaymentResponse does not carry AVS/CVV/3DS/auth-code
            // fields (they are internal to the Authorization entity and not exposed
            // via the API), so those are left null/false here.
            return Optional.of(new TransactionDetail(
                    transactionId,
                    amountMinor == null ? 0L : amountMinor,
                    currency,
                    null,
                    null,
                    false,
                    null));
        } catch (Exception ex) {
            log.warn("[PAYMENT] getTransactionDetail error for transaction={}: {}", transactionId, ex.toString());
            return Optional.empty();
        }
    }
}
