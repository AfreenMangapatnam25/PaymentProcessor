package com.paymentprocessor.limit.domain.enums;

/**
 * Auditable actions recorded for compliance and dispute evidence.
 */
public enum AuditAction {
    LIMIT_CONFIG_CREATED,
    LIMIT_CONFIG_UPDATED,
    LIMIT_CONFIG_DISABLED,
    LIMIT_CHECK_EVALUATED,
    LIMIT_RESERVED,
    LIMIT_COMMITTED,
    LIMIT_RELEASED,
    LIMIT_EXPIRED,
    LIMIT_EXCEEDED
}
