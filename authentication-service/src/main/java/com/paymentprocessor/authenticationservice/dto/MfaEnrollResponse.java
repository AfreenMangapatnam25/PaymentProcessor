package com.paymentprocessor.authenticationservice.dto;

public record MfaEnrollResponse(
        String factorId,
        String secret,
        String otpauthUri) {
}
