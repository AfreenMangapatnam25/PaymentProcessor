package com.paymentprocessor.authenticationservice.dto;

/**
 * Result of a login attempt. {@code status} is either {@code AUTHENTICATED}
 * (tokens present) or {@code MFA_REQUIRED} (challenge present).
 */
public record LoginResponse(String status, TokenResponse tokens, MfaChallenge challenge) {

    public static LoginResponse authenticated(TokenResponse tokens) {
        return new LoginResponse("AUTHENTICATED", tokens, null);
    }

    public static LoginResponse mfaRequired(MfaChallenge challenge) {
        return new LoginResponse("MFA_REQUIRED", null, challenge);
    }
}
