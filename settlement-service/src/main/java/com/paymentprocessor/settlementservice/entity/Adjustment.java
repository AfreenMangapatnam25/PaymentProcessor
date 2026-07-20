package com.paymentprocessor.settlementservice.entity;

import com.paymentprocessor.settlementservice.enums.AdjustmentStatus;
import com.paymentprocessor.settlementservice.enums.AdjustmentType;
import com.paymentprocessor.settlementservice.enums.ApprovalLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A manual credit/debit or correction applied to a merchant's settlement,
 * subject to an approval workflow gated by monetary thresholds.
 */
@Entity
@Table(name = "adjustments", indexes = {
        @Index(name = "idx_adjustment_merchant", columnList = "merchant_id"),
        @Index(name = "idx_adjustment_status", columnList = "status")
})
public class Adjustment extends BaseEntity {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private AdjustmentType type;

    /** Positive magnitude in minor units; direction comes from {@link #type}. */
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "reason_code", nullable = false)
    private String reasonCode;

    @Column(name = "description", length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdjustmentStatus status = AdjustmentStatus.PENDING_APPROVAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_approval_level", length = 20)
    private ApprovalLevel requiredApprovalLevel;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    /** Set once the adjustment has been folded into a settlement batch. */
    @Column(name = "applied_batch_id", length = 40)
    private String appliedBatchId;

    @Column(name = "applied_at")
    private Instant appliedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public AdjustmentType getType() { return type; }
    public void setType(AdjustmentType type) { this.type = type; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AdjustmentStatus getStatus() { return status; }
    public void setStatus(AdjustmentStatus status) { this.status = status; }
    public ApprovalLevel getRequiredApprovalLevel() { return requiredApprovalLevel; }
    public void setRequiredApprovalLevel(ApprovalLevel requiredApprovalLevel) { this.requiredApprovalLevel = requiredApprovalLevel; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getAppliedBatchId() { return appliedBatchId; }
    public void setAppliedBatchId(String appliedBatchId) { this.appliedBatchId = appliedBatchId; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
}
