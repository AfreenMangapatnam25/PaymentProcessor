package com.paymentprocessor.disputeservice.repository;

import com.paymentprocessor.disputeservice.entity.DisputeEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeEventRepository extends JpaRepository<DisputeEvent, Long> {

    /** The full audit timeline for a dispute, oldest first. */
    List<DisputeEvent> findByDisputeIdOrderByCreatedAtAsc(String disputeId);
}
