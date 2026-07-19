package com.paymentprocessor.authenticationservice.dto;

import com.paymentprocessor.authenticationservice.domain.VerificationChannel;
import jakarta.validation.constraints.NotNull;

public record StartVerificationRequest(@NotNull VerificationChannel channel) {
}
