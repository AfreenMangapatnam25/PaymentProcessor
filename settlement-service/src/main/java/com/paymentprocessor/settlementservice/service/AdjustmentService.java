package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.config.SettlementProperties;
import com.paymentprocessor.settlementservice.entity.Adjustment;
import com.paymentprocessor.settlementservice.enums.AdjustmentStatus;
import com.paymentprocessor.settlementservice.enums.ApprovalLevel;
import com.paymentprocessor.settlementservice.exception.ApprovalRequiredException;
import com.paymentprocessor.settlementservice.exception.BadRequestException;
import com.paymentprocessor.settlementservice.exception.ResourceNotFoundException;
import com.paymentprocessor.settlementservice.repository.AdjustmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles manual settlement adjustments and their threshold-based approval
 * workflow. Approved adjustments are later folded into a merchant's settlement
 * batch by the {@link com.paymentprocessor.settlementservice.service.batch.BatchingService}.
 */
@Service
public class AdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(AdjustmentService.class);

    private final AdjustmentRepository repository;
    private final SettlementProperties properties;

    public AdjustmentService(AdjustmentRepository repository, SettlementProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Requests a new adjustment. The required approval level is derived from the
     * amount; AUTO-level adjustments are approved immediately.
     */
    @Transactional
    public Adjustment request(Adjustment adjustment) {
        if (adjustment.getAmountMinor() <= 0) {
            throw new BadRequestException("Adjustment amount must be positive (direction comes from type)");
        }
        if (adjustment.getReasonCode() == null || adjustment.getReasonCode().isBlank()) {
            throw new BadRequestException("Adjustment reason code is required");
        }
        adjustment.setId("adj_" + UUID.randomUUID());
        ApprovalLevel required = requiredLevel(adjustment.getAmountMinor());
        adjustment.setRequiredApprovalLevel(required);
        if (required == ApprovalLevel.AUTO) {
            adjustment.setStatus(AdjustmentStatus.APPROVED);
            adjustment.setApprovedBy("system");
            adjustment.setApprovedAt(Instant.now());
        } else {
            adjustment.setStatus(AdjustmentStatus.PENDING_APPROVAL);
        }
        Adjustment saved = repository.save(adjustment);
        log.info("Adjustment {} requested for merchant {} amount {} {} type {} (requires {})",
                saved.getId(), saved.getMerchantId(), saved.getAmountMinor(), saved.getCurrency(),
                saved.getType(), required);
        return saved;
    }

    /**
     * Approves a pending adjustment. The approver's level must meet or exceed the
     * required level for the amount.
     */
    @Transactional
    public Adjustment approve(String id, String approvedBy, ApprovalLevel approverLevel) {
        Adjustment adjustment = getOrThrow(id);
        if (adjustment.getStatus() != AdjustmentStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Adjustment " + id + " is not pending approval");
        }
        if (!meets(approverLevel, adjustment.getRequiredApprovalLevel())) {
            throw new ApprovalRequiredException("Adjustment " + id + " requires "
                    + adjustment.getRequiredApprovalLevel() + " approval but got " + approverLevel);
        }
        adjustment.setStatus(AdjustmentStatus.APPROVED);
        adjustment.setApprovedBy(approvedBy);
        adjustment.setApprovedAt(Instant.now());
        log.info("Adjustment {} approved by {} ({})", id, approvedBy, approverLevel);
        return repository.save(adjustment);
    }

    @Transactional
    public Adjustment reject(String id, String rejectedBy) {
        Adjustment adjustment = getOrThrow(id);
        if (adjustment.getStatus() != AdjustmentStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Adjustment " + id + " is not pending approval");
        }
        adjustment.setStatus(AdjustmentStatus.REJECTED);
        adjustment.setApprovedBy(rejectedBy);
        adjustment.setApprovedAt(Instant.now());
        log.info("Adjustment {} rejected by {}", id, rejectedBy);
        return repository.save(adjustment);
    }

    /** Approved-but-unapplied adjustments for a merchant + currency. */
    @Transactional(readOnly = true)
    public List<Adjustment> approvedFor(String merchantId, String currency) {
        return repository.findByMerchantIdAndCurrencyAndStatus(merchantId, currency, AdjustmentStatus.APPROVED);
    }

    /** Marks an adjustment as applied to a batch. */
    @Transactional
    public void markApplied(Adjustment adjustment, String batchId) {
        adjustment.setStatus(AdjustmentStatus.APPLIED);
        adjustment.setAppliedBatchId(batchId);
        adjustment.setAppliedAt(Instant.now());
        repository.save(adjustment);
    }

    @Transactional(readOnly = true)
    public Adjustment getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment", id));
    }

    @Transactional(readOnly = true)
    public List<Adjustment> findByMerchant(String merchantId) {
        return repository.findByMerchantId(merchantId);
    }

    @Transactional(readOnly = true)
    public List<Adjustment> findAll() {
        return repository.findAll();
    }

    private ApprovalLevel requiredLevel(long amountMinor) {
        if (amountMinor > properties.getApproval().getFinanceDirectorThresholdMinor()) {
            return ApprovalLevel.FINANCE_DIRECTOR;
        }
        if (amountMinor > properties.getApproval().getSupervisorThresholdMinor()) {
            return ApprovalLevel.SUPERVISOR;
        }
        return ApprovalLevel.AUTO;
    }

    private boolean meets(ApprovalLevel provided, ApprovalLevel required) {
        return provided.ordinal() >= required.ordinal();
    }
}
