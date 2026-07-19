package com.paymentprocessor.disputeservice.repository;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStage;
import com.paymentprocessor.disputeservice.entity.Representment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepresentmentRepository extends JpaRepository<Representment, String> {

    List<Representment> findByDisputeId(String disputeId);

    Optional<Representment> findFirstByDisputeIdAndStageOrderByCreatedAtDesc(
            String disputeId, DisputeStage stage);

    long countByDisputeId(String disputeId);
}
