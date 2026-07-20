package com.paymentprocessor.disputeservice.repository;

import com.paymentprocessor.disputeservice.domain.enums.EvidenceStatus;
import com.paymentprocessor.disputeservice.entity.Evidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, String> {

    List<Evidence> findByDisputeId(String disputeId);

    List<Evidence> findByDisputeIdAndStatus(String disputeId, EvidenceStatus status);

    long countByDisputeIdAndStatus(String disputeId, EvidenceStatus status);
}
