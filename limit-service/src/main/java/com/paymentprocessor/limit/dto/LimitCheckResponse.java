package com.paymentprocessor.limit.dto;

import com.paymentprocessor.limit.domain.enums.LimitDecision;

import java.util.List;

/**
 * Outcome of a limit evaluation.
 *
 * @param decision       APPROVED, DECLINED (a hard limit was hit) or FLAGGED (a soft limit was hit)
 * @param hardViolations hard limits breached — these block the transaction
 * @param softViolations soft limits breached — these only warn
 * @param evaluated      number of limit rules evaluated
 */
public record LimitCheckResponse(
        LimitDecision decision,
        List<LimitViolationDto> hardViolations,
        List<LimitViolationDto> softViolations,
        int evaluated
) {
    public boolean approved() {
        return decision != LimitDecision.DECLINED;
    }
}
