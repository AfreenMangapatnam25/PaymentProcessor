package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;
import com.paymentprocessor.reconciliationservice.domain.ExceptionStatus;
import com.paymentprocessor.reconciliationservice.domain.ResolutionType;
import com.paymentprocessor.reconciliationservice.domain.ReviewQueue;
import com.paymentprocessor.reconciliationservice.domain.SeverityLevel;
import com.paymentprocessor.reconciliationservice.exception.InvalidOperationException;
import com.paymentprocessor.reconciliationservice.exception.ResourceNotFoundException;
import com.paymentprocessor.reconciliationservice.repository.ExceptionRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Manual-reconciliation workflow over exceptions: assign, resolve, escalate, defer, and query. */
@Service
public class ExceptionRecordService {

    private final ExceptionRecordRepository exceptionRecordRepository;

    public ExceptionRecordService(ExceptionRecordRepository exceptionRecordRepository) {
        this.exceptionRecordRepository = exceptionRecordRepository;
    }

    @Transactional(readOnly = true)
    public ExceptionRecord getById(Long id) {
        return exceptionRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exception not found: " + id));
    }

    @Transactional(readOnly = true)
    public ExceptionRecord getByUuid(UUID uuid) {
        return exceptionRecordRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Exception not found: " + uuid));
    }

    @Transactional(readOnly = true)
    public Page<ExceptionRecord> list(ExceptionStatus status, SeverityLevel severity,
                                      ReviewQueue queue, Long runId, Pageable pageable) {
        if (runId != null) {
            return exceptionRecordRepository.findByReconRunId(runId, pageable);
        }
        if (status != null) {
            return exceptionRecordRepository.findByStatus(status, pageable);
        }
        if (severity != null) {
            return exceptionRecordRepository.findBySeverityLevel(severity, pageable);
        }
        if (queue != null) {
            return exceptionRecordRepository.findByReviewQueue(queue, pageable);
        }
        return exceptionRecordRepository.findAll(pageable);
    }

    @Transactional
    public ExceptionRecord assign(Long id, String assignee) {
        ExceptionRecord exception = getById(id);
        ensureOpen(exception);
        exception.setAssignedTo(assignee);
        exception.setStatus(ExceptionStatus.IN_REVIEW);
        return exceptionRecordRepository.save(exception);
    }

    @Transactional
    public ExceptionRecord resolve(Long id, ResolutionType resolutionType, String note, String resolvedBy) {
        ExceptionRecord exception = getById(id);
        if (exception.getStatus() == ExceptionStatus.RESOLVED) {
            throw new InvalidOperationException("Exception " + id + " is already resolved");
        }
        exception.setStatus(ExceptionStatus.RESOLVED);
        exception.setResolutionType(resolutionType);
        exception.setResolutionNote(note);
        exception.setResolvedBy(resolvedBy);
        exception.setResolvedAt(Instant.now());
        return exceptionRecordRepository.save(exception);
    }

    @Transactional
    public ExceptionRecord escalate(Long id, ReviewQueue queue, String note, String actor) {
        ExceptionRecord exception = getById(id);
        exception.setStatus(ExceptionStatus.ESCALATED);
        if (queue != null) {
            exception.setReviewQueue(queue);
            exception.setSlaDueAt(Instant.now().plusSeconds(queue.getSlaHours() * 3600L));
        }
        exception.setResolutionNote(note);
        exception.setAssignedTo(actor);
        return exceptionRecordRepository.save(exception);
    }

    @Transactional
    public ExceptionRecord defer(Long id, String note, String actor) {
        ExceptionRecord exception = getById(id);
        exception.setStatus(ExceptionStatus.DEFERRED);
        exception.setResolutionType(ResolutionType.DEFERRED);
        exception.setResolutionNote(note);
        exception.setResolvedBy(actor);
        return exceptionRecordRepository.save(exception);
    }

    /** Mark an exception resolved as the result of a posted adjustment. */
    @Transactional
    public void markResolvedByAdjustment(ExceptionRecord exception, String actor) {
        exception.setStatus(ExceptionStatus.RESOLVED);
        exception.setResolutionType(ResolutionType.ADJUSTED);
        exception.setResolvedBy(actor);
        exception.setResolvedAt(Instant.now());
        exceptionRecordRepository.save(exception);
    }

    private void ensureOpen(ExceptionRecord exception) {
        if (exception.getStatus() == ExceptionStatus.RESOLVED) {
            throw new InvalidOperationException("Exception " + exception.getId() + " is already resolved");
        }
    }
}
