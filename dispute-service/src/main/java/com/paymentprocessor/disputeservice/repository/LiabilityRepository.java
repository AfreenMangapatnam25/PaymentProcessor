package com.paymentprocessor.disputeservice.repository;

import com.paymentprocessor.disputeservice.entity.Liability;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiabilityRepository extends JpaRepository<Liability, String> {

    List<Liability> findByDisputeId(String disputeId);

    Optional<Liability> findFirstByDisputeIdOrderByRecordedAtDesc(String disputeId);
}
