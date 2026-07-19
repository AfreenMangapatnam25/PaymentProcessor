package com.paymentprocessor.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentprocessor.notificationservice.AbstractIntegrationTest;
import com.paymentprocessor.notificationservice.dto.EventIngestRequest;
import com.paymentprocessor.notificationservice.dto.WebhookEndpointRequest;
import com.paymentprocessor.notificationservice.entity.Event;
import com.paymentprocessor.notificationservice.entity.WebhookDelivery;
import com.paymentprocessor.notificationservice.repository.WebhookDeliveryRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventIngestionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EventIngestionService eventIngestionService;
    @Autowired
    private WebhookEndpointService webhookEndpointService;
    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    private EventIngestRequest request(String id, String merchantId, String type) {
        EventIngestRequest req = new EventIngestRequest();
        req.setId(id);
        req.setMerchantId(merchantId);
        req.setType(type);
        req.setAggregateType("payment");
        req.setAggregateId("pay_1");
        req.setApiVersion("2024-01-01");
        req.setPayload(Map.of("amount", 100));
        return req;
    }

    @Test
    void ingestingTheSameIdTwiceIsIdempotent() {
        String merchantId = "merch_" + System.nanoTime();
        EventIngestRequest req = request("evt_fixed_id_1", merchantId, "payment.succeeded");

        Event first = eventIngestionService.ingest(req);
        Event second = eventIngestionService.ingest(req);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getSequence()).isEqualTo(first.getSequence());
        assertThat(second.getCreatedAt()).isEqualTo(first.getCreatedAt());
    }

    @Test
    void sequenceIsMonotonicPerMerchantAndIndependentAcrossMerchants() {
        String merchantA = "merch_a_" + System.nanoTime();
        String merchantB = "merch_b_" + System.nanoTime();

        Event a1 = eventIngestionService.ingest(request(null, merchantA, "payment.succeeded"));
        Event b1 = eventIngestionService.ingest(request(null, merchantB, "payment.succeeded"));
        Event a2 = eventIngestionService.ingest(request(null, merchantA, "payment.succeeded"));

        assertThat(a1.getSequence()).isEqualTo(1);
        assertThat(a2.getSequence()).isEqualTo(2);
        assertThat(b1.getSequence()).isEqualTo(1);
    }

    @Test
    void fanOutOnlyCreatesDeliveriesForActiveSubscribedEndpoints() {
        String merchantId = "merch_fanout_" + System.nanoTime();

        var subscribed = webhookEndpointService.create(endpointRequest(merchantId, "https://example.com/a",
                new String[]{"payment.succeeded"}));
        var wrongType = webhookEndpointService.create(endpointRequest(merchantId, "https://example.com/b",
                new String[]{"dispute.opened"}));
        var disabled = webhookEndpointService.create(endpointRequest(merchantId, "https://example.com/c",
                new String[]{"payment.succeeded"}));
        webhookEndpointService.setStatus(disabled.getId(), "disabled");

        Event event = eventIngestionService.ingest(request(null, merchantId, "payment.succeeded"));

        List<WebhookDelivery> subscribedDeliveries = webhookDeliveryRepository.findByEndpointId(subscribed.getId());
        List<WebhookDelivery> wrongTypeDeliveries = webhookDeliveryRepository.findByEndpointId(wrongType.getId());
        List<WebhookDelivery> disabledDeliveries = webhookDeliveryRepository.findByEndpointId(disabled.getId());

        assertThat(subscribedDeliveries).hasSize(1);
        assertThat(subscribedDeliveries.get(0).getEventId()).isEqualTo(event.getId());
        assertThat(subscribedDeliveries.get(0).getStatus()).isEqualTo(WebhookDelivery.STATUS_PENDING);
        assertThat(wrongTypeDeliveries).isEmpty();
        assertThat(disabledDeliveries).isEmpty();
    }

    private WebhookEndpointRequest endpointRequest(String merchantId, String url, String[] types) {
        WebhookEndpointRequest req = new WebhookEndpointRequest();
        req.setMerchantId(merchantId);
        req.setUrl(url);
        req.setSubscribedTypes(types);
        req.setApiVersion("2024-01-01");
        return req;
    }
}
