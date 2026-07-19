package com.paymentprocessor.notificationservice.controller;

import com.paymentprocessor.notificationservice.dto.EventIngestRequest;
import com.paymentprocessor.notificationservice.dto.EventResponse;
import com.paymentprocessor.notificationservice.service.EventIngestionService;
import com.paymentprocessor.notificationservice.service.EventService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingestion endpoint for events published by payment-service, ledger-service,
 * merchant-service, settlement-service, and dispute-service. Every ingested
 * event is fanned out to subscribed, active webhook endpoints as pending
 * deliveries in the same transaction.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventIngestionService ingestionService;
    private final EventService eventService;

    public EventController(EventIngestionService ingestionService, EventService eventService) {
        this.ingestionService = ingestionService;
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> ingest(@Valid @RequestBody EventIngestRequest request) {
        EventResponse response = EventResponse.from(ingestionService.ingest(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<EventResponse> byMerchant(@RequestParam String merchantId,
                                           @RequestParam(defaultValue = "50") int limit) {
        return eventService.findByMerchantId(merchantId, limit).stream()
                .map(EventResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable String id) {
        return eventService.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new NoSuchElementException("no event with id " + id));
    }
}
