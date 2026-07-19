package com.paymentprocessor.disputeservice.dto.response;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStage;
import com.paymentprocessor.disputeservice.domain.enums.IssuerResponse;
import com.paymentprocessor.disputeservice.domain.enums.RepresentmentStatus;
import com.paymentprocessor.disputeservice.entity.Representment;
import java.time.Instant;

/**
 * API view of a representment / arbitration filing.
 */
public record RepresentmentResponse(
        String id,
        String disputeId,
        DisputeStage stage,
        RepresentmentStatus status,
        String networkReference,
        int evidenceCount,
        Long feeMinor,
        IssuerResponse issuerResponse,
        String submittedBy,
        Instant submittedAt,
        Instant decidedAt) {

    public static RepresentmentResponse from(Representment r) {
        return new RepresentmentResponse(r.getId(), r.getDisputeId(), r.getStage(), r.getStatus(),
                r.getNetworkReference(), r.getEvidenceCount(), r.getFeeMinor(), r.getIssuerResponse(),
                r.getSubmittedBy(), r.getSubmittedAt(), r.getDecidedAt());
    }
}
