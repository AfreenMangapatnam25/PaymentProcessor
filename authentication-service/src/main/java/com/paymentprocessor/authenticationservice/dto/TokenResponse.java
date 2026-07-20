package com.paymentprocessor.authenticationservice.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        String sessionId) {

    public static TokenResponse bearer(String accessToken, long expiresIn,
                                       String refreshToken, String sessionId) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, refreshToken, sessionId);
    }
}
