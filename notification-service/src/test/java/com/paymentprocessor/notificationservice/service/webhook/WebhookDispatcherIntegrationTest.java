package com.paymentprocessor.notificationservice.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.paymentprocessor.notificationservice.AbstractIntegrationTest;
import com.paymentprocessor.notificationservice.dto.EventIngestRequest;
import com.paymentprocessor.notificationservice.dto.WebhookEndpointRequest;
import com.paymentprocessor.notificationservice.entity.WebhookDelivery;
import com.paymentprocessor.notificationservice.entity.WebhookEndpoint;
import com.paymentprocessor.notificationservice.repository.WebhookDeliveryRepository;
import com.paymentprocessor.notificationservice.repository.WebhookEndpointRepository;
import com.paymentprocessor.notificationservice.service.EventIngestionService;
import com.paymentprocessor.notificationservice.service.WebhookEndpointService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Exercises the dispatcher end to end against a real Postgres (Testcontainers)
 * and a throwaway local HTTP server standing in for a merchant's webhook
 * receiver -- covering the three behaviors called out in the design doc:
 * signed delivery, per-merchant ordering, and endpoint auto-disable.
 */
class WebhookDispatcherIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void dispatcherProperties(DynamicPropertyRegistry registry) {
        // Fast schedule so retry/auto-disable behavior doesn't require real waiting.
        registry.add("notification.dispatcher.backoff-schedule", () -> "50ms,50ms,50ms");
        registry.add("notification.dispatcher.auto-disable-threshold", () -> "2");
    }

    @Autowired
    private EventIngestionService eventIngestionService;
    @Autowired
    private WebhookEndpointService webhookEndpointService;
    @Autowired
    private WebhookEndpointRepository webhookEndpointRepository;
    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;
    @Autowired
    private WebhookDispatcher dispatcher;
    @Autowired
    private SecretResolver secretResolver;
    @Autowired
    private HmacSigner hmacSigner;

    private HttpServer server;
    private final AtomicInteger nextResponseCode = new AtomicInteger(200);
    private final List<CapturedRequest> received = new CopyOnWriteArrayList<>();

    private record CapturedRequest(String body, String signatureHeader) {
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            String signature = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");
            received.add(new CapturedRequest(body, signature));

            int status = nextResponseCode.get();
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String endpointUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
    }

    private WebhookEndpoint createEndpoint(String merchantId) {
        WebhookEndpointRequest req = new WebhookEndpointRequest();
        req.setMerchantId(merchantId);
        req.setUrl(endpointUrl());
        req.setSubscribedTypes(new String[]{"payment.succeeded"});
        req.setApiVersion("2024-01-01");
        return webhookEndpointService.create(req);
    }

    private void ingest(String id, String merchantId) {
        EventIngestRequest req = new EventIngestRequest();
        req.setId(id);
        req.setMerchantId(merchantId);
        req.setType("payment.succeeded");
        req.setAggregateType("payment");
        req.setAggregateId("pay_" + id);
        req.setApiVersion("2024-01-01");
        req.setPayload(Map.of("amount", 100));
        eventIngestionService.ingest(req);
    }

    @Test
    void deliversSuccessfullyWithAValidSignature() {
        String merchantId = "merch_" + System.nanoTime();
        WebhookEndpoint endpoint = createEndpoint(merchantId);
        nextResponseCode.set(200);

        ingest("evt_ok_1", merchantId);
        int attempted = dispatcher.dispatchBatch();

        assertThat(attempted).isEqualTo(1);
        assertThat(received).hasSize(1);

        String secret = secretResolver.resolve(endpoint.getSecretRef());
        boolean valid = hmacSigner.verify(secret, received.get(0).body(), received.get(0).signatureHeader());
        assertThat(valid).isTrue();

        List<WebhookDelivery> deliveries = webhookDeliveryRepository.findByEndpointId(endpoint.getId());
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).getStatus()).isEqualTo(WebhookDelivery.STATUS_DELIVERED);
        assertThat(deliveries.get(0).getResponseCode()).isEqualTo(200);
    }

    @Test
    void doesNotDeliverALaterEventUntilAnEarlierOneForTheSameEndpointResolves() {
        String merchantId = "merch_" + System.nanoTime();
        WebhookEndpoint endpoint = createEndpoint(merchantId);

        nextResponseCode.set(500); // first event will fail and go back to pending
        ingest("evt_order_1", merchantId);
        ingest("evt_order_2", merchantId);

        dispatcher.dispatchBatch(); // claims + attempts evt_order_1 only (it's the lowest sequence)
        assertThat(received).hasSize(1);

        List<WebhookDelivery> afterFirstRound = webhookDeliveryRepository.findByEndpointId(endpoint.getId());
        WebhookDelivery first = afterFirstRound.stream()
                .filter(d -> d.getEventId().equals("evt_order_1")).findFirst().orElseThrow();
        WebhookDelivery second = afterFirstRound.stream()
                .filter(d -> d.getEventId().equals("evt_order_2")).findFirst().orElseThrow();

        assertThat(first.getStatus()).isEqualTo(WebhookDelivery.STATUS_PENDING); // failed, rescheduled
        assertThat(second.getStatus()).isEqualTo(WebhookDelivery.STATUS_PENDING); // never attempted
        assertThat(second.getAttempt()).isZero(); // proves it wasn't claimed, not just that it failed

        // Let evt_order_1 succeed on its retry, then evt_order_2 should become claimable.
        nextResponseCode.set(200);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            dispatcher.dispatchBatch();
            WebhookDelivery updated = webhookDeliveryRepository.findById(second.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(WebhookDelivery.STATUS_DELIVERED);
        });
    }

    @Test
    void autoDisablesTheEndpointAfterConsecutiveFailures() {
        String merchantId = "merch_" + System.nanoTime();
        WebhookEndpoint endpoint = createEndpoint(merchantId);
        nextResponseCode.set(500);

        // auto-disable-threshold is overridden to 2 for this test.
        ingest("evt_fail_1", merchantId);
        dispatcher.dispatchBatch();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            dispatcher.dispatchBatch();
            WebhookEndpoint updated = webhookEndpointRepository.findById(endpoint.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(WebhookEndpoint.STATUS_AUTO_DISABLED);
            assertThat(updated.getConsecutiveFailures()).isGreaterThanOrEqualTo(2);
        });
    }
}
