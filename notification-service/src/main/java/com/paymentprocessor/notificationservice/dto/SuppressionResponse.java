package com.paymentprocessor.notificationservice.dto;

import com.paymentprocessor.notificationservice.entity.Suppression;
import java.time.Instant;

public class SuppressionResponse {
    private String channel;
    private String reason;
    private Instant createdAt;

    public static SuppressionResponse from(Suppression s) {
        SuppressionResponse r = new SuppressionResponse();
        r.channel = s.getChannel();
        r.reason = s.getReason();
        r.createdAt = s.getCreatedAt();
        return r;
    }

    public String getChannel() { return channel; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
