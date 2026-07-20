package com.paymentprocessor.userservice.application.command;

import com.paymentprocessor.userservice.domain.consent.ConsentKind;
import com.paymentprocessor.userservice.domain.consent.SubjectType;

/**
 * Command to revoke consent for a (subject, kind). Idempotent.
 */
public record RevokeConsentCommand(
        SubjectType subjectType,
        String subjectId,
        ConsentKind kind
) {
}
