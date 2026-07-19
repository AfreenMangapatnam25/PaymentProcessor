package com.paymentprocessor.merchantservice.event.outbox;

import com.paymentprocessor.merchantservice.common.enums.OutboxStatus;
import com.paymentprocessor.merchantservice.entity.BaseEntity;
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

/**
 * Transactional outbox row. Domain events are written here in the same DB transaction as the
 * state change that produced them; a relay publishes PENDING rows to Kafka and marks them
 * PUBLISHED, guaranteeing at-least-once delivery with no lost events.
 */
@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "ix_outbox_status_created", columnList = "status, created_at"),
        @Index(name = "ix_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
})
@Getter
@Setter
public class OutboxEvent extends BaseEntity {

    @Column(name = "aggregate_type", nullable = false, length = 60)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    /** Kafka topic the event should be relayed to. */
    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    /** Partition key (typically the aggregate id) to preserve per-merchant ordering. */
    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 2048)
    private String lastError;
}
