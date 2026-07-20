package com.paymentprocessor.userservice.application.command;

import com.paymentprocessor.userservice.domain.consent.ConsentKind;
import com.paymentprocessor.userservice.domain.consent.SubjectType;

/**
 * Command to grant consent for a (subject, kind). Idempotent.
 */
public record GrantConsentCommand(
        SubjectType subjectType,
        String subjectId,
        ConsentKind kind,
        String source,
        String policyVersion
) {
}
