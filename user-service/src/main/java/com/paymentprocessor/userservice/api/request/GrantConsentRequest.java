package com.paymentprocessor.userservice.api.request;

import com.paymentprocessor.userservice.domain.consent.ConsentKind;
import com.paymentprocessor.userservice.domain.consent.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to grant consent for a (subject, kind).
 */
public record GrantConsentRequest(

        @NotNull
        SubjectType subjectType,

        @NotBlank
        String subjectId,

        @NotNull
        ConsentKind kind,

        @Size(max = 64)
        String source,

        @Size(max = 32)
        String policyVersion
) {
}
