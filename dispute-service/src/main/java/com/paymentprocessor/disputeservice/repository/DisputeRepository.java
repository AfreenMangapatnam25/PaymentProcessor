package com.paymentprocessor.disputeservice.repository;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.entity.Dispute;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, String> {

    Optional<Dispute> findByChargebackId(String chargebackId);

    boolean existsByChargebackId(String chargebackId);

    List<Dispute> findByMerchantId(String merchantId);

    List<Dispute> findByMerchantIdAndStatus(String merchantId, DisputeStatus status);

    List<Dispute> findByStatus(DisputeStatus status);

    /** Active disputes whose deadline has already passed — candidates for auto-loss. */
    List<Dispute> findByStatusInAndDeadlineAtBefore(
            Collection<DisputeStatus> statuses, Instant cutoff);

    /** Active disputes whose deadline falls within a window — candidates for reminders. */
    List<Dispute> findByStatusInAndDeadlineAtBetween(
            Collection<DisputeStatus> statuses, Instant from, Instant to);
}
