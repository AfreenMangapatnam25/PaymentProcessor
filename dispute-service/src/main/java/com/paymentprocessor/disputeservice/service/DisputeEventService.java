package com.paymentprocessor.disputeservice.service;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.domain.enums.TimelineEventType;
import com.paymentprocessor.disputeservice.entity.Dispute;
import com.paymentprocessor.disputeservice.entity.DisputeEvent;
import com.paymentprocessor.disputeservice.repository.DisputeEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and serves the immutable audit / status timeline for disputes. Every
 * state change, notification, evidence action and ledger posting is captured
 * here for compliance and network audit purposes.
 */
@Service
public class DisputeEventService {

    /** Conventional actor names for timeline entries. */
    public static final String ACTOR_SYSTEM = "SYSTEM";
    public static final String ACTOR_PLATFORM = "PLATFORM";
    public static final String ACTOR_MERCHANT = "MERCHANT";
    public static final String ACTOR_NETWORK = "NETWORK";

    private final DisputeEventRepository repository;

    public DisputeEventService(DisputeEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Records a timeline event that is not a status change.
     */
    @Transactional
    public DisputeEvent record(Dispute dispute, TimelineEventType type,
                               String actor, String description) {
        return record(dispute, type, actor, description, null, null);
    }

    /**
     * Records a status transition on the timeline.
     */
    @Transactional
    public DisputeEvent recordStatusChange(Dispute dispute, DisputeStatus from,
                                           DisputeStatus to, String actor,
                                           String description) {
        return record(dispute, TimelineEventType.STATUS_CHANGED, actor, description, from, to);
    }

    @Transactional
    public DisputeEvent record(Dispute dispute, TimelineEventType type, String actor,
                               String description, DisputeStatus from, DisputeStatus to) {
        DisputeEvent event = new DisputeEvent();
        event.setDisputeId(dispute.getId());
        event.setType(type);
        event.setActor(actor);
        event.setDescription(description);
        event.setFromStatus(from);
        event.setToStatus(to);
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<DisputeEvent> getTimeline(String disputeId) {
        return repository.findByDisputeIdOrderByCreatedAtAsc(disputeId);
    }
}
