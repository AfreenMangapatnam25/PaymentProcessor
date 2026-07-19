package com.paymentprocessor.disputeservice.dto.response;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.domain.enums.TimelineEventType;
import com.paymentprocessor.disputeservice.entity.DisputeEvent;
import java.time.Instant;

/**
 * API view of a single audit-timeline entry.
 */
public record TimelineEventResponse(
        Long id,
        String disputeId,
        TimelineEventType type,
        String actor,
        String description,
        DisputeStatus fromStatus,
        DisputeStatus toStatus,
        Instant createdAt) {

    public static TimelineEventResponse from(DisputeEvent e) {
        return new TimelineEventResponse(e.getId(), e.getDisputeId(), e.getType(), e.getActor(),
                e.getDescription(), e.getFromStatus(), e.getToStatus(), e.getCreatedAt());
    }
}
