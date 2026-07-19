package com.paymentprocessor.notificationservice.dto;

import com.paymentprocessor.notificationservice.entity.WebhookEndpoint;
import java.time.Instant;

public class WebhookEndpointResponse {
    private String id;
    private String merchantId;
    private String url;
    private String secretRef;
    private String[] subscribedTypes;
    private String apiVersion;
    private String status;
    private int consecutiveFailures;
    private Instant createdAt;

    public static WebhookEndpointResponse from(WebhookEndpoint e) {
        WebhookEndpointResponse r = new WebhookEndpointResponse();
        r.id = e.getId();
        r.merchantId = e.getMerchantId();
        r.url = e.getUrl();
        r.secretRef = e.getSecretRef();
        r.subscribedTypes = e.getSubscribedTypes();
        r.apiVersion = e.getApiVersion();
        r.status = e.getStatus();
        r.consecutiveFailures = e.getConsecutiveFailures();
        r.createdAt = e.getCreatedAt();
        return r;
    }

    public String getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public String getUrl() { return url; }
    public String getSecretRef() { return secretRef; }
    public String[] getSubscribedTypes() { return subscribedTypes; }
    public String getApiVersion() { return apiVersion; }
    public String getStatus() { return status; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public Instant getCreatedAt() { return createdAt; }
}
