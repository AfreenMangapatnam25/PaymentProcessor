package com.paymentprocessor.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record IntrospectRequest(@NotBlank String token) {
}
