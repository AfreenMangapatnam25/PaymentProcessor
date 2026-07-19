package com.paymentprocessor.reconciliationservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Metadata for an ingested external bank statement file. The individual line items are normalized
 * into {@link ReconRecord} rows with source = EXTERNAL for matching.
 */
@Entity
@Table(name = "bank_statement", indexes = {
        @Index(name = "idx_bank_statement_ref", columnList = "statement_reference", unique = true),
        @Index(name = "idx_bank_statement_account_date", columnList = "account_number, statement_date")
})
@Getter
@Setter
public class BankStatement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_reference", nullable = false, unique = true, length = 128)
    private String statementReference;

    @Column(name = "bank_name", nullable = false, length = 128)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 24)
    private AccountType accountType;

    @Column(name = "account_number", nullable = false, length = 64)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 16)
    private StatementFormat format;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "opening_balance", precision = 20, scale = 4)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", precision = 20, scale = 4)
    private BigDecimal closingBalance;

    @Column(name = "record_count", nullable = false)
    private int recordCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StatementStatus status = StatementStatus.INGESTED;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt = Instant.now();
}
