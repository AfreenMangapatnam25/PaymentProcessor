package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.Adjustment;
import com.paymentprocessor.reconciliationservice.domain.AdjustmentStatus;
import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;
import com.paymentprocessor.reconciliationservice.exception.InvalidOperationException;
import com.paymentprocessor.reconciliationservice.exception.ResourceNotFoundException;
import com.paymentprocessor.reconciliationservice.repository.AdjustmentRepository;
import com.paymentprocessor.reconciliationservice.repository.ExceptionRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages corrective adjustments. Adjustments above the configured threshold require dual approval
 * with segregation of duties (approver must differ from creator) before they can be posted. Posting
 * an adjustment resolves the linked exception.
 */
@Slf4j
@Service
public class AdjustmentService {

    private final AdjustmentRepository adjustmentRepository;
    private final ExceptionRecordRepository exceptionRecordRepository;
    private final ExceptionRecordService exceptionRecordService;
    private final ReconProperties properties;

    public AdjustmentService(AdjustmentRepository adjustmentRepository,
                             ExceptionRecordRepository exceptionRecordRepository,
                             ExceptionRecordService exceptionRecordService,
                             ReconProperties properties) {
        this.adjustmentRepository = adjustmentRepository;
        this.exceptionRecordRepository = exceptionRecordRepository;
        this.exceptionRecordService = exceptionRecordService;
        this.properties = properties;
    }

    @Transactional
    public Adjustment create(Long exceptionId, Adjustment adjustment) {
        ExceptionRecord exception = exceptionRecordRepository.findById(exceptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Exception not found: " + exceptionId));
        adjustment.setExceptionRecord(exception);

        boolean requiresDualApproval = adjustment.getAmount() != null
                && adjustment.getAmount().abs().compareTo(properties.getSeverity().getDualApprovalAmount()) > 0;
        adjustment.setRequiresDualApproval(requiresDualApproval);
        adjustment.setStatus(requiresDualApproval ? AdjustmentStatus.PENDING_APPROVAL : AdjustmentStatus.PENDING);

        Adjustment saved = adjustmentRepository.save(adjustment);
        log.info("Created adjustment {} for exception {} amount={} dualApproval={}",
                saved.getId(), exception.getUuid(), saved.getAmount(), requiresDualApproval);
        return saved;
    }

    @Transactional
    public Adjustment approve(Long id, String approver) {
        Adjustment adjustment = getById(id);
        if (adjustment.getStatus() != AdjustmentStatus.PENDING_APPROVAL) {
            throw new InvalidOperationException(
                    "Adjustment " + id + " is not awaiting approval (status " + adjustment.getStatus() + ")");
        }
        if (approver != null && approver.equalsIgnoreCase(adjustment.getCreatedBy())) {
            throw new InvalidOperationException(
                    "Segregation of duties: approver must differ from the creator");
        }
        adjustment.setStatus(AdjustmentStatus.APPROVED);
        adjustment.setApprovedBy(approver);
        adjustment.setApprovedAt(Instant.now());
        return adjustmentRepository.save(adjustment);
    }

    /**
     * Post an approved (or non-dual-approval PENDING) adjustment to the ledger and resolve the linked
     * exception. In production the {@code ledgerReference} is returned by the Ledger Service.
     */
    @Transactional
    public Adjustment post(Long id, String actor) {
        Adjustment adjustment = getById(id);
        boolean postable = adjustment.getStatus() == AdjustmentStatus.APPROVED
                || (adjustment.getStatus() == AdjustmentStatus.PENDING && !adjustment.isRequiresDualApproval());
        if (!postable) {
            throw new InvalidOperationException(
                    "Adjustment " + id + " cannot be posted from status " + adjustment.getStatus());
        }
        adjustment.setStatus(AdjustmentStatus.POSTED);
        adjustment.setPostedAt(Instant.now());
        adjustment.setLedgerReference("LEDGER-" + UUID.randomUUID());
        Adjustment saved = adjustmentRepository.save(adjustment);

        exceptionRecordService.markResolvedByAdjustment(adjustment.getExceptionRecord(), actor);
        log.info("Posted adjustment {} ledgerRef={} resolving exception {}",
                saved.getId(), saved.getLedgerReference(), adjustment.getExceptionRecord().getUuid());
        return saved;
    }

    @Transactional
    public Adjustment reject(Long id, String actor) {
        Adjustment adjustment = getById(id);
        if (adjustment.getStatus() == AdjustmentStatus.POSTED) {
            throw new InvalidOperationException("Adjustment " + id + " is already posted");
        }
        adjustment.setStatus(AdjustmentStatus.REJECTED);
        adjustment.setApprovedBy(actor);
        adjustment.setApprovedAt(Instant.now());
        return adjustmentRepository.save(adjustment);
    }

    @Transactional(readOnly = true)
    public Adjustment getById(Long id) {
        return adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Adjustment> listByException(Long exceptionId) {
        return adjustmentRepository.findByExceptionRecordId(exceptionId);
    }
}
