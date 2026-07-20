package com.paymentprocessor.disputeservice.dto.response;

import java.util.List;

/**
 * Full dispute view: the dispute itself plus its evidence, representments,
 * financial impact and audit timeline.
 */
public record DisputeDetailResponse(
        DisputeResponse dispute,
        LiabilityResponse liability,
        List<EvidenceResponse> evidence,
        List<RepresentmentResponse> representments,
        List<TimelineEventResponse> timeline) {
}
