package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.BeneficialOwnerRole;
import com.paymentprocessor.merchantservice.common.enums.KycStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BeneficialOwnerResponse(
        UUID id, UUID merchantId, String firstName, String lastName, LocalDate dateOfBirth,
        String email, BeneficialOwnerRole role, BigDecimal ownershipPercentage,
        String nationality, KycStatus kycStatus
) {}
