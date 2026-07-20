package com.paymentprocessor.authenticationservice.dto;

import com.paymentprocessor.authenticationservice.domain.IdentityStatus;
import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import com.paymentprocessor.authenticationservice.entity.Identity;

import java.time.Instant;

public record IdentityResponse(
        String id,
        PrincipalType principalType,
        String email,
        String phoneE164,
        IdentityStatus status,
        boolean mfaRequired,
        boolean emailVerified,
        Instant createdAt) {

    public static IdentityResponse from(Identity i) {
        return new IdentityResponse(i.getId(), i.getPrincipalType(), i.getEmail(), i.getPhoneE164(),
                i.getStatus(), i.isMfaRequired(), i.getEmailVerifiedAt() != null, i.getCreatedAt());
    }
}
