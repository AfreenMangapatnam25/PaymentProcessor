package com.paymentprocessor.disputeservice.entity;

import com.paymentprocessor.disputeservice.domain.enums.DisputeSource;
import com.paymentprocessor.disputeservice.domain.enums.DisputeStage;
import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.domain.enums.DisputeType;
import com.paymentprocessor.disputeservice.domain.enums.LiabilityParty;
import com.paymentprocessor.disputeservice.domain.enums.Network;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root representing a single payment dispute across its full
 * lifecycle. The Dispute Service owns this record; references to transactions,
 * payments, merchants and customers are held by id only, as those records are
 * owned by other services.
 */
@Entity
@Table(name = "disputes", indexes = {
        @Index(name = "idx_dispute_merchant", columnList = "merchant_id"),
        @Index(name = "idx_dispute_status", columnList = "status"),
        @Index(name = "idx_dispute_deadline", columnList = "deadline_at"),
        @Index(name = "idx_dispute_chargeback", columnList = "chargeback_id", unique = true)
})
public class Dispute {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    /** Network-assigned chargeback reference (e.g. Visa TC40, Mastercard ARD). */
    @Column(name = "chargeback_id", nullable = false, unique = true)
    private String chargebackId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "customer_id")
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false, length = 20)
    private Network network;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private DisputeType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30)
    private DisputeSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private DisputeStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DisputeStatus status;

    @Column(name = "reason_code", length = 20)
    private String reasonCode;

    @Column(name = "reason_description", length = 500)
    private String reasonDescription;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "chargeback_fee_minor")
    private Long chargebackFeeMinor;

    @Column(name = "is_partial")
    private boolean partial;

    @Enumerated(EnumType.STRING)
    @Column(name = "liability_party", length = 20)
    private LiabilityParty liabilityParty = LiabilityParty.PENDING;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    /** Network-imposed deadline for the current stage. */
    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** Whether the merchant has been alerted about the current stage. */
    @Column(name = "merchant_notified")
    private boolean merchantNotified;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // ----- lifecycle callbacks ------------------------------------------------

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = "dsp_" + UUID.randomUUID().toString().replace("-", "");
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (receivedAt == null) {
            receivedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ----- domain helpers -----------------------------------------------------

    /** @return true if the response deadline has passed relative to {@code now}. */
    public boolean isPastDeadline(Instant now) {
        return deadlineAt != null && now.isAfter(deadlineAt);
    }

    /** @return whole days remaining until the deadline (negative if overdue). */
    public long daysUntilDeadline(Instant now) {
        if (deadlineAt == null) {
            return Long.MAX_VALUE;
        }
        return Duration.between(now, deadlineAt).toDays();
    }

    /** @return total exposure (disputed amount + chargeback fee) in minor units. */
    public long totalExposureMinor() {
        long fee = chargebackFeeMinor == null ? 0L : chargebackFeeMinor;
        long amount = amountMinor == null ? 0L : amountMinor;
        return amount + fee;
    }

    // ----- getters / setters --------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChargebackId() { return chargebackId; }
    public void setChargebackId(String chargebackId) { this.chargebackId = chargebackId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Network getNetwork() { return network; }
    public void setNetwork(Network network) { this.network = network; }
    public DisputeType getType() { return type; }
    public void setType(DisputeType type) { this.type = type; }
    public DisputeSource getSource() { return source; }
    public void setSource(DisputeSource source) { this.source = source; }
    public DisputeStage getStage() { return stage; }
    public void setStage(DisputeStage stage) { this.stage = stage; }
    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    public Long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(Long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getChargebackFeeMinor() { return chargebackFeeMinor; }
    public void setChargebackFeeMinor(Long chargebackFeeMinor) { this.chargebackFeeMinor = chargebackFeeMinor; }
    public boolean isPartial() { return partial; }
    public void setPartial(boolean partial) { this.partial = partial; }
    public LiabilityParty getLiabilityParty() { return liabilityParty; }
    public void setLiabilityParty(LiabilityParty liabilityParty) { this.liabilityParty = liabilityParty; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public boolean isMerchantNotified() { return merchantNotified; }
    public void setMerchantNotified(boolean merchantNotified) { this.merchantNotified = merchantNotified; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
