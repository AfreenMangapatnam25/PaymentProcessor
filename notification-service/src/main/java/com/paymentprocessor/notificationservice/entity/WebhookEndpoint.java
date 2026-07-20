package com.paymentprocessor.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DISABLED = "disabled";
    public static final String STATUS_AUTO_DISABLED = "auto_disabled";

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "secret_ref", nullable = false)
    private String secretRef;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "subscribed_types", columnDefinition = "text[]", nullable = false)
    private String[] subscribedTypes;

    @Column(name = "api_version", nullable = false)
    private String apiVersion;

    @Column(name = "status", nullable = false)
    private String status = STATUS_ACTIVE;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSecretRef() { return secretRef; }
    public void setSecretRef(String secretRef) { this.secretRef = secretRef; }
    public String[] getSubscribedTypes() { return subscribedTypes; }
    public void setSubscribedTypes(String[] subscribedTypes) { this.subscribedTypes = subscribedTypes; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean subscribesTo(String eventType) {
        if (subscribedTypes == null) {
            return false;
        }
        for (String t : subscribedTypes) {
            if (t.equals(eventType)) {
                return true;
            }
        }
        return false;
    }
}
