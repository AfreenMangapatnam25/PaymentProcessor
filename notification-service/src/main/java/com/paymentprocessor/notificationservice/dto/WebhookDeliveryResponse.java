package com.paymentprocessor.notificationservice.dto;

import com.paymentprocessor.notificationservice.entity.WebhookDelivery;
import java.time.Instant;

public class WebhookDeliveryResponse {
    private long id;
    private String endpointId;
    private String eventId;
    private int attempt;
    private String status;
    private Instant nextRetryAt;
    private Integer responseCode;
    private Integer responseMs;
    private String error;
    private Instant createdAt;

    public static WebhookDeliveryResponse from(WebhookDelivery d) {
        WebhookDeliveryResponse r = new WebhookDeliveryResponse();
        r.id = d.getId();
        r.endpointId = d.getEndpointId();
        r.eventId = d.getEventId();
        r.attempt = d.getAttempt();
        r.status = d.getStatus();
        r.nextRetryAt = d.getNextRetryAt();
        r.responseCode = d.getResponseCode();
        r.responseMs = d.getResponseMs();
        r.error = d.getError();
        r.createdAt = d.getCreatedAt();
        return r;
    }

    public long getId() { return id; }
    public String getEndpointId() { return endpointId; }
    public String getEventId() { return eventId; }
    public int getAttempt() { return attempt; }
    public String getStatus() { return status; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Integer getResponseCode() { return responseCode; }
    public Integer getResponseMs() { return responseMs; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }
}
