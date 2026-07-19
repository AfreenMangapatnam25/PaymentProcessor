package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.BeneficialOwnerRole;
import com.paymentprocessor.merchantservice.common.enums.KycStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A beneficial owner, director, or authorized signatory of the merchant, with KYC status. */
@Entity
@Table(name = "beneficial_owner", indexes = {
        @Index(name = "ix_owner_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class BeneficialOwner extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "email", length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private BeneficialOwnerRole role;

    @Column(name = "ownership_percentage", precision = 5, scale = 2)
    private BigDecimal ownershipPercentage;

    @Column(name = "nationality", length = 2)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;
}
