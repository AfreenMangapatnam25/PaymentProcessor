package com.paymentprocessor.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** notification.retention.* -- partition maintenance for events / webhook_deliveries. */
@ConfigurationProperties(prefix = "notification.retention")
public class RetentionProperties {

    private int retentionDays = 90;
    private int monthsAheadToCreate = 2;

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public int getMonthsAheadToCreate() { return monthsAheadToCreate; }
    public void setMonthsAheadToCreate(int monthsAheadToCreate) { this.monthsAheadToCreate = monthsAheadToCreate; }
}
