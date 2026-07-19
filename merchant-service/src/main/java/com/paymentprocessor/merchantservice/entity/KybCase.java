package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.KybCaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A Know-Your-Business verification case. The Merchant Service tracks the case and its
 * outcome; the actual verification logic is owned by the Compliance/KYB Service.
 */
@Entity
@Table(name = "kyb_case", indexes = {
        @Index(name = "ix_kybcase_merchant", columnList = "merchant_id"),
        @Index(name = "ix_kybcase_status", columnList = "status")
})
@Getter
@Setter
public class KybCase extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KybCaseStatus status = KybCaseStatus.OPEN;

    /** Correlation id of the case in the external Compliance/KYB Service. */
    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "sanctions_screened", nullable = false)
    private boolean sanctionsScreened = false;

    @Column(name = "pep_screened", nullable = false)
    private boolean pepScreened = false;

    @Column(name = "decision_reason", length = 1024)
    private String decisionReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
