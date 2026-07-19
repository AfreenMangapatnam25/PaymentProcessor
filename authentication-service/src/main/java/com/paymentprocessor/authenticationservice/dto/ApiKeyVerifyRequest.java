package com.paymentprocessor.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ApiKeyVerifyRequest(@NotBlank String apiKey) {
}
