package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.*;
import com.paymentprocessor.reconciliationservice.event.ReconEventPublisher;
import com.paymentprocessor.reconciliationservice.exception.InvalidOperationException;
import com.paymentprocessor.reconciliationservice.exception.ResourceNotFoundException;
import com.paymentprocessor.reconciliationservice.repository.ExceptionRecordRepository;
import com.paymentprocessor.reconciliationservice.repository.MatchRepository;
import com.paymentprocessor.reconciliationservice.repository.ReconRecordRepository;
import com.paymentprocessor.reconciliationservice.repository.ReconRunRepository;
import com.paymentprocessor.reconciliationservice.service.matching.MatchingEngine;
import com.paymentprocessor.reconciliationservice.service.matching.MatchingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the reconciliation lifecycle: create a run, ingest records, then execute the
 * match-detect pipeline (duplicate detection, rule-based matching, mismatch classification and
 * severity scoring), persist results, and publish domain events.
 */
@Slf4j
@Service
public class ReconRunService {

    private final ReconRunRepository reconRunRepository;
    private final ReconRecordRepository reconRecordRepository;
    private final MatchRepository matchRepository;
    private final ExceptionRecordRepository exceptionRecordRepository;
    private final MatchingEngine matchingEngine;
    private final MismatchDetectionService mismatchDetectionService;
    private final ReconEventPublisher eventPublisher;
    private final ReconProperties properties;

    public ReconRunService(ReconRunRepository reconRunRepository,
                           ReconRecordRepository reconRecordRepository,
                           MatchRepository matchRepository,
                           ExceptionRecordRepository exceptionRecordRepository,
                           MatchingEngine matchingEngine,
                           MismatchDetectionService mismatchDetectionService,
                           ReconEventPublisher eventPublisher,
                           ReconProperties properties) {
        this.reconRunRepository = reconRunRepository;
        this.reconRecordRepository = reconRecordRepository;
        this.matchRepository = matchRepository;
        this.exceptionRecordRepository = exceptionRecordRepository;
        this.matchingEngine = matchingEngine;
        this.mismatchDetectionService = mismatchDetectionService;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Transactional
    public ReconRun createRun(ReconRun run) {
        run.setStatus(ReconRunStatus.PENDING);
        ReconRun saved = reconRunRepository.save(run);
        log.info("Created recon run {} type={} channel={} date={}",
                saved.getUuid(), saved.getReconType(), saved.getChannel(), saved.getBusinessDate());
        return saved;
    }

    @Transactional(readOnly = true)
    public ReconRun getRun(Long id) {
        return reconRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation run not found: " + id));
    }

    @Transactional(readOnly = true)
    public ReconRun getRunByUuid(UUID uuid) {
        return reconRunRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation run not found: " + uuid));
    }

    @Transactional(readOnly = true)
    public Page<ReconRun> listRuns(Pageable pageable) {
        return reconRunRepository.findAll(pageable);
    }

    /**
     * Execute the full reconciliation pipeline for a PENDING run. Idempotency guard: a run may only be
     * executed once.
     */
    @Transactional
    public ReconRun execute(Long runId) {
        ReconRun run = getRun(runId);
        if (run.getStatus() != ReconRunStatus.PENDING) {
            throw new InvalidOperationException(
                    "Run " + runId + " cannot be executed from status " + run.getStatus());
        }

        run.setStatus(ReconRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        reconRunRepository.save(run);

        try {
            List<ReconRecord> internals = reconRecordRepository
                    .findByReconRunIdAndSource(runId, RecordSource.INTERNAL);
            List<ReconRecord> externals = reconRecordRepository
                    .findByReconRunIdAndSource(runId, RecordSource.EXTERNAL);

            // 1. Duplicate detection on each side.
            var dupInternal = mismatchDetectionService.detectDuplicates(run, internals);
            var dupExternal = mismatchDetectionService.detectDuplicates(run, externals);

            // 2. Rule-based matching on the deduplicated records.
            MatchingResult matching = matchingEngine.match(run, dupInternal.unique(), dupExternal.unique());
            matchRepository.saveAll(matching.matches());

            // 3. Classify everything still unmatched.
            List<ExceptionRecord> classified = mismatchDetectionService.classifyUnmatched(
                    run, matching.unmatchedInternal(), matching.unmatchedExternal());

            List<ExceptionRecord> allExceptions = new ArrayList<>();
            allExceptions.addAll(dupInternal.exceptions());
            allExceptions.addAll(dupExternal.exceptions());
            allExceptions.addAll(classified);
            exceptionRecordRepository.saveAll(allExceptions);

            // Persist match-status changes on records.
            reconRecordRepository.saveAll(internals);
            reconRecordRepository.saveAll(externals);

            applyCounters(run, internals, externals, matching, allExceptions);
            run.setStatus(ReconRunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            reconRunRepository.save(run);

            eventPublisher.publishReconciliationCompleted(run);
            allExceptions.forEach(eventPublisher::publishMismatchDetected);

            log.info("Completed recon run {}: matched={} exceptions={} matchRate={}",
                    run.getUuid(), run.getMatchedCount(), run.getExceptionCount(), run.getMatchRate());
            return run;
        } catch (RuntimeException ex) {
            log.error("Reconciliation run {} failed", run.getUuid(), ex);
            run.setStatus(ReconRunStatus.FAILED);
            run.setFailureReason(truncate(ex.getMessage()));
            run.setCompletedAt(Instant.now());
            reconRunRepository.save(run);
            throw ex;
        }
    }

    private void applyCounters(ReconRun run, List<ReconRecord> internals, List<ReconRecord> externals,
                               MatchingResult matching, List<ExceptionRecord> exceptions) {
        long duplicates = exceptions.stream()
                .filter(e -> e.getCategory() == MismatchCategory.DUPLICATE).count();
        long missingInternal = exceptions.stream()
                .filter(e -> e.getCategory() == MismatchCategory.MISSING_INTERNAL).count();
        long missingExternal = exceptions.stream()
                .filter(e -> e.getCategory() == MismatchCategory.MISSING_EXTERNAL).count();
        long amountMismatch = exceptions.stream()
                .filter(e -> e.getCategory() == MismatchCategory.AMOUNT_MISMATCH).count();

        run.setTotalInternal(internals.size());
        run.setTotalExternal(externals.size());
        run.setMatchedCount(matching.matches().size());
        run.setDuplicateCount(duplicates);
        run.setMissingInternalCount(missingInternal);
        run.setMissingExternalCount(missingExternal);
        run.setMismatchedCount(amountMismatch);
        run.setExceptionCount(exceptions.size());

        BigDecimal matchedAmount = matching.matches().stream()
                .map(m -> m.getInternalRecord().getAmount())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        run.setMatchedAmount(matchedAmount);

        long denominator = run.getMatchedCount() + run.getExceptionCount();
        if (denominator > 0) {
            run.setMatchRate(BigDecimal.valueOf(run.getMatchedCount())
                    .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP));
        } else {
            run.setMatchRate(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    /** Convenience for tests / callers needing the active-status set. */
    public static EnumSet<ReconRunStatus> activeStatuses() {
        return EnumSet.of(ReconRunStatus.PENDING, ReconRunStatus.RUNNING);
    }
}
