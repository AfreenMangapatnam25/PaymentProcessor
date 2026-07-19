package com.paymentprocessor.limit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.limit.domain.entity.LimitAuditLog;
import com.paymentprocessor.limit.domain.enums.AuditAction;
import com.paymentprocessor.limit.repository.LimitAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Writes the compliance audit trail. Records are written in a {@code REQUIRES_NEW}
 * transaction so an audit entry survives even if the surrounding business
 * transaction later rolls back (e.g. a declined reservation still leaves evidence).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final LimitAuditLogRepository auditRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, String entityReference, String transactionId,
                       String actor, Object detail) {
        try {
            LimitAuditLog entry = LimitAuditLog.builder()
                    .action(action)
                    .entityReference(entityReference)
                    .transactionId(transactionId)
                    .actor(actor == null ? "limit-service" : actor)
                    .detail(serialize(detail))
                    .createdAt(Instant.now())
                    .build();
            auditRepository.save(entry);
        } catch (Exception ex) {
            // Auditing must never break the primary flow.
            log.error("Failed to write audit log action={} ref={}: {}",
                    action, entityReference, ex.getMessage(), ex);
        }
    }

    private String serialize(Object detail) {
        if (detail == null) {
            return null;
        }
        if (detail instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return String.valueOf(detail);
        }
    }
}
