package com.paymentprocessor.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One row per (endpoint, event): the delivery/retry state machine.
 * Physical PK is (id, created_at) in Postgres for partitioning reasons; id
 * alone (a bigserial-backed identity shared across all partitions) is
 * globally unique and is what JPA uses as entity identity.
 */
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_DELIVERING = "delivering";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_DEAD = "dead";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint_id", nullable = false)
    private String endpointId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "attempt", nullable = false)
    private Integer attempt = 0;

    @Column(name = "status", nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_ms")
    private Integer responseMs;

    @Column(name = "error")
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEndpointId() { return endpointId; }
    public void setEndpointId(String endpointId) { this.endpointId = endpointId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Integer getAttempt() { return attempt; }
    public void setAttempt(Integer attempt) { this.attempt = attempt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
    public Integer getResponseMs() { return responseMs; }
    public void setResponseMs(Integer responseMs) { this.responseMs = responseMs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
