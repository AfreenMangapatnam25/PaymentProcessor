package com.paymentprocessor.notificationservice.dto;

import com.paymentprocessor.notificationservice.entity.Event;
import java.time.Instant;
import java.util.Map;

public class EventResponse {
    private String id;
    private String merchantId;
    private String type;
    private String aggregateType;
    private String aggregateId;
    private String apiVersion;
    private Map<String, Object> payload;
    private long sequence;
    private Instant createdAt;

    public static EventResponse from(Event e) {
        EventResponse r = new EventResponse();
        r.id = e.getId();
        r.merchantId = e.getMerchantId();
        r.type = e.getType();
        r.aggregateType = e.getAggregateType();
        r.aggregateId = e.getAggregateId();
        r.apiVersion = e.getApiVersion();
        r.payload = e.getPayload();
        r.sequence = e.getSequence();
        r.createdAt = e.getCreatedAt();
        return r;
    }

    public String getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public String getType() { return type; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getApiVersion() { return apiVersion; }
    public Map<String, Object> getPayload() { return payload; }
    public long getSequence() { return sequence; }
    public Instant getCreatedAt() { return createdAt; }
}
