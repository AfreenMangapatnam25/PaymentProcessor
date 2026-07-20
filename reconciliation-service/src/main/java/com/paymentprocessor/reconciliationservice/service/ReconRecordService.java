package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRun;
import com.paymentprocessor.reconciliationservice.domain.ReconRunStatus;
import com.paymentprocessor.reconciliationservice.exception.InvalidOperationException;
import com.paymentprocessor.reconciliationservice.exception.ResourceNotFoundException;
import com.paymentprocessor.reconciliationservice.repository.ReconRecordRepository;
import com.paymentprocessor.reconciliationservice.repository.ReconRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Ingests and queries the internal/external records attached to a reconciliation run. */
@Service
public class ReconRecordService {

    private final ReconRecordRepository reconRecordRepository;
    private final ReconRunRepository reconRunRepository;

    public ReconRecordService(ReconRecordRepository reconRecordRepository,
                              ReconRunRepository reconRunRepository) {
        this.reconRecordRepository = reconRecordRepository;
        this.reconRunRepository = reconRunRepository;
    }

    /** Attach a batch of records to a run. Only permitted while the run is still PENDING. */
    @Transactional
    public List<ReconRecord> addRecords(Long runId, List<ReconRecord> records) {
        ReconRun run = reconRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation run not found: " + runId));
        if (run.getStatus() != ReconRunStatus.PENDING) {
            throw new InvalidOperationException(
                    "Cannot add records to run " + runId + " in status " + run.getStatus());
        }
        records.forEach(r -> r.setReconRun(run));
        return reconRecordRepository.saveAll(records);
    }

    @Transactional(readOnly = true)
    public Page<ReconRecord> listRecords(Long runId, Pageable pageable) {
        if (!reconRunRepository.existsById(runId)) {
            throw new ResourceNotFoundException("Reconciliation run not found: " + runId);
        }
        return reconRecordRepository.findByReconRunId(runId, pageable);
    }

    @Transactional(readOnly = true)
    public ReconRecord getRecord(Long id) {
        return reconRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation record not found: " + id));
    }
}
