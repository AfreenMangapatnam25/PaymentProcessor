package com.paymentprocessor.auditservice.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.paymentprocessor.auditservice.domain.AuditBatch;

/** API view of a sealed daily batch manifest. */
public record BatchResponse(
        String id,
        LocalDate batchDate,
        long fromSeq,
        long toSeq,
        long recordCount,
        String rootHash,
        String signature,
        String signingKeyId,
        String s3Bucket,
        String s3Key,
        String s3VersionId,
        Instant retainUntil,
        String anchorRef,
        String status,
        Instant createdAt,
        Instant sealedAt) {

    public static BatchResponse from(AuditBatch b) {
        return new BatchResponse(
                b.getId(), b.getBatchDate(), b.getFromSeq(), b.getToSeq(), b.getRecordCount(),
                b.getRootHash(), b.getSignature(), b.getSigningKeyId(), b.getS3Bucket(),
                b.getS3Key(), b.getS3VersionId(), b.getRetainUntil(), b.getAnchorRef(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getCreatedAt(), b.getSealedAt());
    }
}
