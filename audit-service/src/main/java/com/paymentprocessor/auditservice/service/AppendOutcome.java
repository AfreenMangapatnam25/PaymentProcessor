package com.paymentprocessor.auditservice.service;

import com.paymentprocessor.auditservice.domain.AuditRecord;

/**
 * Result of an append: the stored record and whether it was newly created (as opposed
 * to an idempotent replay of an existing {@code eventId}).
 */
public record AppendOutcome(AuditRecord record, boolean created) {
}
