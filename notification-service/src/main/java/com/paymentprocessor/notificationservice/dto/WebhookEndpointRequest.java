package com.paymentprocessor.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class WebhookEndpointRequest {

    @NotBlank
    private String merchantId;

    @NotBlank
    private String url;

    @NotEmpty
    private String[] subscribedTypes;

    @NotBlank
    private String apiVersion;

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String[] getSubscribedTypes() { return subscribedTypes; }
    public void setSubscribedTypes(String[] subscribedTypes) { this.subscribedTypes = subscribedTypes; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
}
