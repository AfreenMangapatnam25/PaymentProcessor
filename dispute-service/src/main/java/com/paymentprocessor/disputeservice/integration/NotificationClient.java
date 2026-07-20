package com.paymentprocessor.disputeservice.integration;

import com.paymentprocessor.disputeservice.domain.enums.NotificationChannel;
import com.paymentprocessor.disputeservice.domain.enums.NotificationUrgency;
import java.util.Set;

/**
 * Outbound port to the Notification Service for alerting merchants about dispute
 * activity, deadlines and outcomes across the appropriate channels.
 */
public interface NotificationClient {

    /**
     * Sends a merchant notification.
     *
     * @param merchantId target merchant
     * @param disputeId  related dispute
     * @param subject    short subject / event label
     * @param body       rendered message body
     * @param urgency    drives escalation behaviour
     * @param channels   channels to deliver on
     */
    void notifyMerchant(String merchantId, String disputeId, String subject,
                        String body, NotificationUrgency urgency,
                        Set<NotificationChannel> channels);
}
