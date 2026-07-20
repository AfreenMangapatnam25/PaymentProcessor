package com.paymentprocessor.auditservice.api.dto;

import java.time.Instant;
import java.util.Map;

import com.paymentprocessor.auditservice.domain.Actor;
import com.paymentprocessor.auditservice.domain.AuditRecord;
import com.paymentprocessor.auditservice.domain.ResourceRef;

/** API view of a stored audit record. */
public record AuditRecordResponse(
        String id,
        long seq,
        Instant ts,
        Instant recordedAt,
        Actor actor,
        String action,
        ResourceRef resource,
        String merchantId,
        Map<String, Object> before,
        Map<String, Object> after,
        String requestId,
        String traceId,
        String eventId,
        String prevHash,
        String hash,
        String batchId) {

    public static AuditRecordResponse from(AuditRecord r) {
        return new AuditRecordResponse(
                r.getId(), r.getSeq(), r.getTs(), r.getRecordedAt(), r.getActor(), r.getAction(),
                r.getResource(), r.getMerchantId(), r.getBefore(), r.getAfter(), r.getRequestId(),
                r.getTraceId(), r.getEventId(), r.getPrevHash(), r.getHash(), r.getBatchId());
    }
}
