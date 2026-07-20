package com.paymentprocessor.userservice.application.query;

import com.paymentprocessor.userservice.domain.consent.SubjectType;

/**
 * Query for all consent records of a subject.
 */
public record ListConsentsQuery(SubjectType subjectType, String subjectId) {
}
