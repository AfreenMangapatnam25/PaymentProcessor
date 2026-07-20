package com.paymentprocessor.userservice.domain.consent;

import com.paymentprocessor.userservice.common.exception.ValidationException;

/**
 * The subject a consent belongs to: a type plus the subject's id, with the id
 * prefix validated against the type so a mismatched pair cannot be built.
 */
public record ConsentSubject(SubjectType type, String id) {

    public ConsentSubject {
        if (type == null) {
            throw new ValidationException("subjectType", "Subject type is required");
        }
        if (id == null || id.isBlank()) {
            throw new ValidationException("subjectId", "Subject id is required");
        }
        if (!id.startsWith(type.idPrefix())) {
            throw new ValidationException("subjectId",
                    "Subject id must start with '" + type.idPrefix() + "' for subject type " + type);
        }
    }

    public static ConsentSubject of(SubjectType type, String id) {
        return new ConsentSubject(type, id);
    }
}
