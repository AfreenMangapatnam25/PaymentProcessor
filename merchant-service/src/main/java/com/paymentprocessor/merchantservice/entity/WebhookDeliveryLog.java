package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.WebhookDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Record of an attempted delivery of an event to a merchant webhook endpoint. */
@Entity
@Table(name = "webhook_delivery_log", indexes = {
        @Index(name = "ix_wdl_webhook", columnList = "webhook_id"),
        @Index(name = "ix_wdl_status", columnList = "status")
})
@Getter
@Setter
public class WebhookDeliveryLog extends BaseEntity {

    @Column(name = "webhook_id", nullable = false)
    private UUID webhookId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WebhookDeliveryStatus status = WebhookDeliveryStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;
}
