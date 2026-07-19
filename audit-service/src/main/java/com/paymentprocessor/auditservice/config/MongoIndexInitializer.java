package com.paymentprocessor.auditservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import com.paymentprocessor.auditservice.domain.AuditRecord;

/**
 * Creates the indexes the audit query copy relies on, explicitly at startup rather than
 * via auto-index-creation (which is discouraged in production). Index creation is
 * idempotent, so this is safe to run on every boot.
 *
 * <p>Includes the query indexes from the data model plus the two uniqueness constraints
 * that enforce integrity: {@code seq} (serialises the chain) and {@code event_id}
 * (idempotency / dedup).
 */
@Component
public class MongoIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongo;

    public MongoIndexInitializer(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        IndexOperations ops = mongo.indexOps(AuditRecord.class);

        // Uniqueness / integrity constraints.
        ops.ensureIndex(new Index().on("seq", Sort.Direction.ASC).unique().named("uk_seq"));
        ops.ensureIndex(new Index().on("event_id", Sort.Direction.ASC).unique()
                .sparse().named("uk_event_id"));

        // Query access patterns from the data model.
        ops.ensureIndex(new Index()
                .on("merchant_id", Sort.Direction.ASC)
                .on("ts", Sort.Direction.DESC)
                .named("ix_merchant_ts"));
        ops.ensureIndex(new Index()
                .on("resource.type", Sort.Direction.ASC)
                .on("resource.id", Sort.Direction.ASC)
                .on("ts", Sort.Direction.DESC)
                .named("ix_resource_ts"));
        ops.ensureIndex(new Index()
                .on("action", Sort.Direction.ASC)
                .on("ts", Sort.Direction.DESC)
                .named("ix_action_ts"));

        // Supports daily batch selection by ingest window.
        ops.ensureIndex(new Index()
                .on("recordedAt", Sort.Direction.ASC)
                .named("ix_recorded_at"));

        log.info("Ensured MongoDB indexes for audit_records.");
    }
}
