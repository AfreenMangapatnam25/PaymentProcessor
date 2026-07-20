package com.paymentprocessor.disputeservice.dto.response;

import com.paymentprocessor.disputeservice.domain.enums.EvidenceCategory;
import com.paymentprocessor.disputeservice.domain.enums.EvidenceStatus;
import com.paymentprocessor.disputeservice.domain.enums.EvidenceType;
import com.paymentprocessor.disputeservice.entity.Evidence;
import java.time.Instant;

/**
 * API view of a piece of evidence.
 */
public record EvidenceResponse(
        String id,
        String disputeId,
        String fileName,
        EvidenceType type,
        EvidenceCategory category,
        Long sizeBytes,
        EvidenceStatus status,
        String description,
        boolean malwareScanned,
        String uploadedBy,
        Instant uploadedAt,
        Instant reviewedAt,
        Instant submittedAt) {

    public static EvidenceResponse from(Evidence e) {
        return new EvidenceResponse(e.getId(), e.getDisputeId(), e.getFileName(), e.getType(),
                e.getCategory(), e.getSizeBytes(), e.getStatus(), e.getDescription(),
                e.isMalwareScanned(), e.getUploadedBy(), e.getUploadedAt(), e.getReviewedAt(),
                e.getSubmittedAt());
    }
}
