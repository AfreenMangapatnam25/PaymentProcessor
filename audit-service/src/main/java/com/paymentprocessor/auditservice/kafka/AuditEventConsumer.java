package com.paymentprocessor.auditservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.auditservice.api.dto.CreateAuditRecordRequest;
import com.paymentprocessor.auditservice.service.AppendOutcome;
import com.paymentprocessor.auditservice.service.AuditIngestionService;
import com.paymentprocessor.auditservice.service.exception.InvalidAuditEventException;

/**
 * Async ingestion path: consumes audit events from Kafka and appends them to the chain.
 *
 * <p>Offsets are committed manually only after the record is durably appended, so a
 * crash mid-processing replays the message rather than losing it. Because ingestion is
 * idempotent on {@code eventId}, at-least-once redelivery does not create duplicates.
 * Non-retryable failures (validation, PII) are routed to the DLT by the configured
 * error handler; transient failures are retried with backoff.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditIngestionService ingestion;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditIngestionService ingestion, ObjectMapper objectMapper) {
        this.ingestion = ingestion;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${audit.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${audit.kafka.enabled:true}")
    public void onMessage(@Payload String payload,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                          Acknowledgment ack) {
        CreateAuditRecordRequest request = parse(payload);
        AppendOutcome outcome = ingestion.append(request.toCommand());
        ack.acknowledge();
        log.debug("Kafka audit event ingested: id={} seq={} created={} key={}",
                outcome.record().getId(), outcome.record().getSeq(), outcome.created(), key);
    }

    private CreateAuditRecordRequest parse(String payload) {
        try {
            return objectMapper.readValue(payload, CreateAuditRecordRequest.class);
        } catch (Exception e) {
            // Not retryable — malformed JSON will never succeed; send to DLT.
            throw new InvalidAuditEventException("Malformed audit event payload: " + e.getMessage());
        }
    }
}
