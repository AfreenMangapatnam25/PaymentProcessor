package com.paymentprocessor.disputeservice.dto.response;

import com.paymentprocessor.disputeservice.entity.ReasonCodeCatalog;

/**
 * API view of a reason-code catalogue entry.
 */
public record ReasonCodeResponse(
        String network,
        String code,
        String category,
        String description,
        String winRate,
        String requiredEvidence,
        String optionalEvidence,
        Integer responseDays) {

    public static ReasonCodeResponse from(ReasonCodeCatalog r) {
        return new ReasonCodeResponse(r.getNetwork(), r.getCode(), r.getCategory(),
                r.getDescription(), r.getWinRate(), r.getRequiredEvidence(),
                r.getOptionalEvidence(), r.getResponseDays());
    }
}
