package com.paymentprocessor.notificationservice.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.notificationservice.entity.Event;
import com.paymentprocessor.notificationservice.entity.WebhookDelivery;
import com.paymentprocessor.notificationservice.entity.WebhookEndpoint;
import com.paymentprocessor.notificationservice.repository.EventRepository;
import com.paymentprocessor.notificationservice.repository.WebhookEndpointRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Core outbound-webhook queue worker.
 *
 * Each poll:
 *  1. reclaims any 'delivering' rows whose claim visibility has expired
 *     (crash recovery),
 *  2. atomically claims a batch of due, in-order deliveries
 *     (WebhookDeliveryRepository#claimBatch does the FOR UPDATE SKIP LOCKED
 *     + per-endpoint ordering-guard work in one statement),
 *  3. attempts each claimed delivery over HTTP *outside* any DB transaction
 *     (never hold a row lock or open connection across network I/O),
 *  4. finalizes each attempt (delivered / rescheduled with backoff / dead)
 *     via WebhookDispatchTransactions, in its own short transaction.
 */
@Component
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final WebhookEndpointRepository endpointRepository;
    private final EventRepository eventRepository;
    private final SecretResolver secretResolver;
    private final HmacSigner hmacSigner;
    private final WebhookHttpClient httpClient;
    private final WebhookDispatchTransactions transactions;
    private final ObjectMapper objectMapper;

    public WebhookDispatcher(WebhookEndpointRepository endpointRepository,
                              EventRepository eventRepository,
                              SecretResolver secretResolver,
                              HmacSigner hmacSigner,
                              WebhookHttpClient httpClient,
                              WebhookDispatchTransactions transactions,
                              ObjectMapper objectMapper) {
        this.endpointRepository = endpointRepository;
        this.eventRepository = eventRepository;
        this.secretResolver = secretResolver;
        this.hmacSigner = hmacSigner;
        this.httpClient = httpClient;
        this.transactions = transactions;
        this.objectMapper = objectMapper;
    }

    /** Runs one poll cycle. Returns the number of deliveries attempted. */
    public int dispatchBatch() {
        List<WebhookDelivery> claimed = transactions.claimDue();
        for (WebhookDelivery delivery : claimed) {
            try {
                processOne(delivery);
            } catch (Exception e) {
                // A bug here must not stop the rest of the batch (or the next poll) from running.
                log.error("unhandled error processing delivery {}", delivery.getId(), e);
            }
        }
        return claimed.size();
    }

    private void processOne(WebhookDelivery delivery) {
        Optional<WebhookEndpoint> endpointOpt = endpointRepository.findById(delivery.getEndpointId());
        if (endpointOpt.isEmpty()) {
            transactions.finalizeDead(delivery, null, "endpoint no longer exists");
            return;
        }
        WebhookEndpoint endpoint = endpointOpt.get();
        if (!endpoint.isActive()) {
            transactions.finalizeDead(delivery, endpoint, "endpoint is " + endpoint.getStatus());
            return;
        }

        Optional<Event> eventOpt = eventRepository.findById(delivery.getEventId());
        if (eventOpt.isEmpty()) {
            transactions.finalizeDead(delivery, endpoint, "source event no longer exists");
            return;
        }
        Event event = eventOpt.get();

        String body;
        try {
            body = objectMapper.writeValueAsString(envelope(event));
        } catch (Exception e) {
            transactions.finalizeDead(delivery, endpoint, "failed to serialize event payload: " + e.getMessage());
            return;
        }

        String secret = secretResolver.resolve(endpoint.getSecretRef());
        String signature = hmacSigner.sign(secret, body, Instant.now());

        WebhookDeliveryResult result = httpClient.post(
                endpoint.getUrl(), body, signature, event.getId(), event.getType(), delivery.getId());

        if (result.isSuccess()) {
            transactions.finalizeSuccess(delivery, endpoint, result);
        } else {
            transactions.finalizeFailure(delivery, endpoint, result);
        }
    }

    private Map<String, Object> envelope(Event event) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", event.getId());
        envelope.put("type", event.getType());
        envelope.put("aggregate_type", event.getAggregateType());
        envelope.put("aggregate_id", event.getAggregateId());
        envelope.put("api_version", event.getApiVersion());
        envelope.put("created_at", event.getCreatedAt().toString());
        envelope.put("sequence", event.getSequence());
        envelope.put("data", event.getPayload());
        return envelope;
    }
}
