package com.paymentprocessor.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** notification.security.* */
@ConfigurationProperties(prefix = "notification.security")
public class SecurityProperties {

    /**
     * Shared secret upstream services (payment-service, ledger-service, etc.)
     * must send as X-Internal-Api-Key when calling this service's ingestion
     * and admin endpoints. Set via INTERNAL_API_KEY env var in real
     * deployments; a blank value disables the check (local dev only).
     */
    private String internalApiKey = "";

    public String getInternalApiKey() { return internalApiKey; }
    public void setInternalApiKey(String internalApiKey) { this.internalApiKey = internalApiKey; }
}
