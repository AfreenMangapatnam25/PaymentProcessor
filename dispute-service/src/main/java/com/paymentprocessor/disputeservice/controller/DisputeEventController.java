package com.paymentprocessor.disputeservice.controller;

import com.paymentprocessor.disputeservice.dto.response.TimelineEventResponse;
import com.paymentprocessor.disputeservice.service.DisputeEventService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only REST API exposing a dispute's audit / status timeline.
 */
@RestController
@RequestMapping("/api/v1/disputes/{disputeId}")
public class DisputeEventController {

    private final DisputeEventService eventService;

    public DisputeEventController(DisputeEventService eventService) {
        this.eventService = eventService;
    }

    /** Returns the full audit timeline for a dispute, oldest first. */
    @GetMapping("/timeline")
    public List<TimelineEventResponse> timeline(@PathVariable String disputeId) {
        return eventService.getTimeline(disputeId).stream()
                .map(TimelineEventResponse::from).toList();
    }
}
