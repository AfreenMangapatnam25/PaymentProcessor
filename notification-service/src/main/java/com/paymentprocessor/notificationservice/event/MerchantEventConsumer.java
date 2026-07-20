package com.paymentprocessor.notificationservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.notificationservice.dto.EventIngestRequest;
import com.paymentprocessor.notificationservice.service.EventIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes the real {@code merchant.events} topic published by merchant-service's
 * transactional outbox ({@code OutboxWriter} / {@code OutboxRelay}, envelope shape
 * {eventId, eventType, aggregateType, aggregateId, occurredAt, schemaVersion, data}).
 *
 * <p>Each event is merchant-scoped by construction (aggregateType "merchant",
 * aggregateId the merchant id), which is exactly the shape {@link EventIngestionService}
 * expects: it records the event and fans it out to any webhook endpoints the merchant
 * has subscribed to that event type. This is the one inbound Kafka wiring in
 * notification-service today -- other candidate topics (e.g. authentication-service's
 * {@code auth.account.locked}) are identity-scoped, not merchant-scoped, and don't carry
 * a resolvable recipient address, so they are intentionally not consumed here yet.
 *
 * <p>Offsets are committed manually only after {@link EventIngestionService#ingest}
 * returns successfully, and ingestion is idempotent on the event id, so at-least-once
 * redelivery on a crash mid-processing does not create duplicate webhook deliveries.
 */
@Component
public class MerchantEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MerchantEventConsumer.class);

    private final EventIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    public MerchantEventConsumer(EventIngestionService ingestionService, ObjectMapper objectMapper) {
        this.ingestionService = ingestionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${events.kafka.merchant-events-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${events.kafka.enabled:true}")
    public void onMessage(String payload, Acknowledgment ack) {
        EventIngestRequest request = toIngestRequest(payload);
        if (request == null) {
            // Malformed/unrecognized envelope: not retryable, skip and move on rather
            // than blocking the partition forever on a message that will never parse.
            ack.acknowledge();
            return;
        }
        ingestionService.ingest(request);
        ack.acknowledge();
        log.debug("Ingested merchant event {} type={} merchant={}",
                request.getId(), request.getType(), request.getMerchantId());
    }

    @SuppressWarnings("unchecked")
    private EventIngestRequest toIngestRequest(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (!"merchant".equals(node.path("aggregateType").asText(null))) {
                return null;
            }
            EventIngestRequest request = new EventIngestRequest();
            request.setId(node.path("eventId").asText(null));
            request.setMerchantId(node.path("aggregateId").asText(null));
            request.setType(node.path("eventType").asText(null));
            request.setAggregateType(node.path("aggregateType").asText(null));
            request.setAggregateId(node.path("aggregateId").asText(null));
            request.setApiVersion("v" + node.path("schemaVersion").asInt(1));
            request.setPayload(objectMapper.convertValue(node.path("data"), java.util.Map.class));
            if (request.getMerchantId() == null || request.getType() == null) {
                return null;
            }
            return request;
        } catch (Exception e) {
            log.warn("Discarding malformed merchant.events payload: {}", e.getMessage());
            return null;
        }
    }
}
