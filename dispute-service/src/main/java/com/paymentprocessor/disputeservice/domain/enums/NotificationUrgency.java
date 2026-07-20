package com.paymentprocessor.disputeservice.domain.enums;

/**
 * Relative urgency of a merchant notification, driving channel selection.
 */
public enum NotificationUrgency {
    STANDARD,
    HIGH,
    CRITICAL,
    IMMEDIATE
}
