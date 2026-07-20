package com.paymentprocessor.limit.exception;

import com.paymentprocessor.limit.dto.LimitViolationDto;
import lombok.Getter;

import java.util.List;

/**
 * Thrown when one or more HARD limits are breached and the transaction must be
 * declined. Carries the specific violations so the caller can surface a precise
 * decline reason.
 */
@Getter
public class LimitExceededException extends RuntimeException {

    private final transient List<LimitViolationDto> violations;

    public LimitExceededException(List<LimitViolationDto> violations) {
        super("Transaction exceeds configured limits: " + violations.size() + " violation(s)");
        this.violations = violations;
    }
}
