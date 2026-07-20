package com.paymentprocessor.reconciliationservice.repository;

import com.paymentprocessor.reconciliationservice.domain.ReconRun;
import com.paymentprocessor.reconciliationservice.domain.ReconRunStatus;
import com.paymentprocessor.reconciliationservice.domain.ReconType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReconRunRepository extends JpaRepository<ReconRun, Long> {

    Optional<ReconRun> findByUuid(java.util.UUID uuid);

    Page<ReconRun> findByStatus(ReconRunStatus status, Pageable pageable);

    Page<ReconRun> findByReconType(ReconType reconType, Pageable pageable);

    Page<ReconRun> findByReconTypeAndBusinessDate(ReconType reconType, LocalDate businessDate, Pageable pageable);

    boolean existsByReconTypeAndChannelAndBusinessDateAndStatusIn(
            ReconType reconType, String channel, LocalDate businessDate, java.util.Collection<ReconRunStatus> statuses);
}
