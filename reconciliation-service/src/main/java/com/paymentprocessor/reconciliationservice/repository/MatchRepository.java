package com.paymentprocessor.reconciliationservice.repository;

import com.paymentprocessor.reconciliationservice.domain.MatchRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<MatchRecord, Long> {

    Page<MatchRecord> findByReconRunId(Long reconRunId, Pageable pageable);

    long countByReconRunId(Long reconRunId);
}
