package com.paymentprocessor.userservice.application.query;

import com.paymentprocessor.userservice.domain.consent.ConsentKind;
import com.paymentprocessor.userservice.domain.consent.SubjectType;

/**
 * Query for one consent record by subject and kind.
 */
public record GetConsentQuery(SubjectType subjectType, String subjectId, ConsentKind kind) {
}
