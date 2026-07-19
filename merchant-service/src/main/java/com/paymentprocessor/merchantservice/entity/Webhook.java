package com.paymentprocessor.merchantservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A merchant-registered webhook endpoint. Subscribed event types are stored as a
 * comma-separated list of {@code WebhookEventType} names. The signing secret is stored
 * hashed and only revealed once at creation.
 */
@Entity
@Table(name = "webhook", indexes = {
        @Index(name = "ix_webhook_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class Webhook extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "endpoint_url", nullable = false, length = 1024)
    private String endpointUrl;

    @Column(name = "subscribed_events", nullable = false, length = 1024)
    private String subscribedEvents;

    /** Hash of the HMAC signing secret. Plaintext is shown only once at creation. */
    @Column(name = "secret_hash", nullable = false, length = 200)
    private String secretHash;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 5;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds = 10;
}
