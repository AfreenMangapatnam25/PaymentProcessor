package com.paymentprocessor.reconciliationservice.repository;

import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import com.paymentprocessor.reconciliationservice.domain.RecordMatchStatus;
import com.paymentprocessor.reconciliationservice.domain.RecordSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconRecordRepository extends JpaRepository<ReconRecord, Long> {

    List<ReconRecord> findByReconRunIdAndSource(Long reconRunId, RecordSource source);

    List<ReconRecord> findByReconRunIdAndSourceAndMatchStatus(
            Long reconRunId, RecordSource source, RecordMatchStatus matchStatus);

    Page<ReconRecord> findByReconRunId(Long reconRunId, Pageable pageable);

    long countByReconRunIdAndSource(Long reconRunId, RecordSource source);

    long countByReconRunIdAndMatchStatus(Long reconRunId, RecordMatchStatus matchStatus);
}
