package com.paymentprocessor.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request body for POST /api/events, published by upstream services
 * (payment-service, ledger-service, merchant-service, settlement-service,
 * dispute-service).
 *
 * id is optional: if the caller supplies one (recommended, so retried
 * publishes are idempotent), it's used verbatim; otherwise the service
 * generates one.
 */
public class EventIngestRequest {

    private String id;

    @NotBlank
    private String merchantId;

    @NotBlank
    private String type;

    @NotBlank
    private String aggregateType;

    @NotBlank
    private String aggregateId;

    @NotBlank
    private String apiVersion;

    @NotNull
    private Map<String, Object> payload;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
