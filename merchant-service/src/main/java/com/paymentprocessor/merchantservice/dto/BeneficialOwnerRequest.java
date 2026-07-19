package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.BeneficialOwnerRole;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BeneficialOwnerRequest(
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        @Past LocalDate dateOfBirth,
        @Email @Size(max = 255) String email,
        @NotNull BeneficialOwnerRole role,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal ownershipPercentage,
        @Size(min = 2, max = 2) String nationality
) {}
