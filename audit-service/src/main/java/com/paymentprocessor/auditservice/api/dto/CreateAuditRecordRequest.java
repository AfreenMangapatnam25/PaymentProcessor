package com.paymentprocessor.auditservice.api.dto;

import java.time.Instant;
import java.util.Map;

import com.paymentprocessor.auditservice.service.AuditAppendCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body to append one audit record.
 *
 * <p>{@code eventId} is the idempotency key: send a stable value (e.g. the source
 * event's id) so retries never create duplicate records. {@code before}/{@code after}
 * must contain only redacted references — the service rejects raw PII.
 */
public record CreateAuditRecordRequest(
        String eventId,
        Instant ts,
        @NotNull @Valid ActorDto actor,
        @NotBlank String action,
        @NotNull @Valid ResourceDto resource,
        String merchantId,
        Map<String, Object> before,
        Map<String, Object> after,
        String requestId,
        String traceId) {

    public AuditAppendCommand toCommand() {
        return new AuditAppendCommand(
                eventId,
                ts,
                actor.toDomain(),
                action,
                resource.toDomain(),
                merchantId,
                before,
                after,
                requestId,
                traceId);
    }
}
