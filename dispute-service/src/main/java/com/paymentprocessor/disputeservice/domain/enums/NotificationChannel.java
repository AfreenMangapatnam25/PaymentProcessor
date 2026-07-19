package com.paymentprocessor.disputeservice.domain.enums;

/**
 * Channels through which merchants are notified about dispute activity.
 */
public enum NotificationChannel {
    EMAIL,
    SMS,
    DASHBOARD,
    WEBHOOK,
    PHONE
}
