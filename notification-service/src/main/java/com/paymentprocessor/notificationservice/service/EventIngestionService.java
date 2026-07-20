package com.paymentprocessor.notificationservice.service;

import com.paymentprocessor.notificationservice.dto.EventIngestRequest;
import com.paymentprocessor.notificationservice.entity.Event;
import com.paymentprocessor.notificationservice.entity.WebhookDelivery;
import com.paymentprocessor.notificationservice.entity.WebhookEndpoint;
import com.paymentprocessor.notificationservice.repository.EventRepository;
import com.paymentprocessor.notificationservice.repository.WebhookDeliveryRepository;
import com.paymentprocessor.notificationservice.repository.WebhookEndpointRepository;
import com.paymentprocessor.notificationservice.util.IdGenerator;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests events from upstream services and fans them out to subscribed,
 * active webhook endpoints as pending deliveries.
 *
 * Idempotency: because events.id can't carry a standalone UNIQUE constraint
 * on a table partitioned by created_at, uniqueness is claimed first against
 * the small event_idempotency table (see V1__init_schema.sql). A caller that
 * retries a publish with the same id gets back the original event instead of
 * creating a duplicate / re-fanning-out deliveries.
 */
@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final EventRepository eventRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final SequenceAllocator sequenceAllocator;
    private final JdbcTemplate jdbcTemplate;

    public EventIngestionService(EventRepository eventRepository,
                                  WebhookEndpointRepository endpointRepository,
                                  WebhookDeliveryRepository deliveryRepository,
                                  SequenceAllocator sequenceAllocator,
                                  JdbcTemplate jdbcTemplate) {
        this.eventRepository = eventRepository;
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.sequenceAllocator = sequenceAllocator;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Event ingest(EventIngestRequest request) {
        String id = (request.getId() != null && !request.getId().isBlank())
                ? request.getId()
                : IdGenerator.generate("evt");
        Instant createdAt = Instant.now();

        int claimed = jdbcTemplate.update(
                "INSERT INTO event_idempotency (event_id, event_created_at) VALUES (?, ?) ON CONFLICT (event_id) DO NOTHING",
                id, Timestamp.from(createdAt));

        if (claimed == 0) {
            return fetchExistingByIdempotencyKey(id);
        }

        long sequence = sequenceAllocator.nextSequence(request.getMerchantId());

        Event event = new Event();
        event.setId(id);
        event.setMerchantId(request.getMerchantId());
        event.setType(request.getType());
        event.setAggregateType(request.getAggregateType());
        event.setAggregateId(request.getAggregateId());
        event.setApiVersion(request.getApiVersion());
        event.setPayload(request.getPayload());
        event.setSequence(sequence);
        event.setCreatedAt(createdAt);
        eventRepository.save(event);

        fanOut(event);
        return event;
    }

    private Event fetchExistingByIdempotencyKey(String id) {
        Timestamp existingCreatedAt = jdbcTemplate.queryForObject(
                "SELECT event_created_at FROM event_idempotency WHERE event_id = ?", Timestamp.class, id);
        return eventRepository.findByIdAndCreatedAt(id, existingCreatedAt.toInstant())
                .orElseThrow(() -> new IllegalStateException(
                        "event_idempotency claimed for " + id + " but events row not visible yet; retry"));
    }

    private void fanOut(Event event) {
        List<WebhookEndpoint> subscribers =
                endpointRepository.findActiveSubscribers(event.getMerchantId(), event.getType());

        for (WebhookEndpoint endpoint : subscribers) {
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setEndpointId(endpoint.getId());
            delivery.setEventId(event.getId());
            delivery.setAttempt(0);
            delivery.setStatus(WebhookDelivery.STATUS_PENDING);
            delivery.setNextRetryAt(Instant.now());
            delivery.setCreatedAt(Instant.now());
            deliveryRepository.save(delivery);
        }

        log.debug("fanned out event {} ({}) to {} endpoint(s)", event.getId(), event.getType(), subscribers.size());
    }
}
