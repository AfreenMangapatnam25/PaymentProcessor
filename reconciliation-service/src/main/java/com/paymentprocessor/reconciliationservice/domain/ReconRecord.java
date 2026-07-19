package com.paymentprocessor.reconciliationservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A normalized transaction record participating in a reconciliation run. Records come from either
 * an internal system (Payment/Ledger/Settlement) or an external statement, and are compared by the
 * matching engine.
 */
@Entity
@Table(name = "recon_record", indexes = {
        @Index(name = "idx_recon_record_run", columnList = "recon_run_id"),
        @Index(name = "idx_recon_record_run_ref", columnList = "recon_run_id, external_reference"),
        @Index(name = "idx_recon_record_run_amount", columnList = "recon_run_id, amount"),
        @Index(name = "idx_recon_record_match_status", columnList = "recon_run_id, match_status")
})
@Getter
@Setter
public class ReconRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recon_run_id", nullable = false)
    private ReconRun reconRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private RecordSource source;

    /** Originating system, e.g. "PAYMENT_SERVICE", "VISA_VSS", "ACME_BANK_MT940". */
    @Column(name = "source_system", length = 64)
    private String sourceSystem;

    /** Primary external reference used for matching (ARN, gateway txn id, bank ref, etc.). */
    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(name = "arn", length = 64)
    private String arn;

    @Column(name = "internal_payment_id", length = 64)
    private String internalPaymentId;

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee_amount", precision = 20, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "merchant_id", length = 64)
    private String merchantId;

    @Column(name = "counterparty", length = 128)
    private String counterparty;

    @Column(name = "card_bin", length = 8)
    private String cardBin;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    /** Business status from the source system, e.g. authorized, captured, refunded, chargeback. */
    @Column(name = "transaction_status", length = 32)
    private String transactionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 16)
    private RecordMatchStatus matchStatus = RecordMatchStatus.UNMATCHED;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;
}
