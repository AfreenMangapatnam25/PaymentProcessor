package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.AccountVerificationStatus;
import com.paymentprocessor.merchantservice.common.enums.BankAccountClass;
import com.paymentprocessor.merchantservice.common.enums.BankAccountPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Bank account where processed funds are settled. The full account number is stored
 * encrypted at rest ({@link #accountNumberEncrypted}); only {@link #accountNumberLast4}
 * is ever exposed through APIs.
 */
@Entity
@Table(name = "settlement_account", indexes = {
        @Index(name = "ix_settlement_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class SettlementAccount extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private BankAccountPurpose purpose = BankAccountPurpose.SETTLEMENT;

    @Column(name = "account_holder_name", nullable = false, length = 255)
    private String accountHolderName;

    @Column(name = "bank_name", length = 255)
    private String bankName;

    @Column(name = "bank_code", length = 40)
    private String bankCode;

    @Column(name = "routing_number", length = 40)
    private String routingNumber;

    @Column(name = "swift", length = 20)
    private String swift;

    @Column(name = "iban_last4", length = 4)
    private String ibanLast4;

    /** AES-GCM encrypted full account number (never returned by APIs). */
    @Column(name = "account_number_encrypted", nullable = false, length = 1024)
    private String accountNumberEncrypted;

    @Column(name = "account_number_last4", nullable = false, length = 4)
    private String accountNumberLast4;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_class", length = 20)
    private BankAccountClass accountClass;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAccount = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private AccountVerificationStatus verificationStatus = AccountVerificationStatus.UNVERIFIED;
}
