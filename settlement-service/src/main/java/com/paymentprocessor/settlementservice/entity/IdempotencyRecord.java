package com.paymentprocessor.settlementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stores the outcome of a mutating API request keyed by a client-supplied
 * {@code Idempotency-Key}, so that a retried request returns the original
 * result instead of performing the operation twice.
 */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord extends BaseEntity {

    @Id
    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 128)
    private String requestFingerprint;

    @Column(name = "resource_type", length = 40)
    private String resourceType;

    @Column(name = "resource_id", length = 40)
    private String resourceId;

    @Column(name = "response_status")
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public int getResponseStatus() { return responseStatus; }
    public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
}
