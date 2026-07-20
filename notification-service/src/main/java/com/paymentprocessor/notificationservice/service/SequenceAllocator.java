package com.paymentprocessor.notificationservice.service;

import java.sql.PreparedStatement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates the next monotonic-per-merchant `events.sequence` value.
 *
 * Uses a transaction-scoped Postgres advisory lock keyed by merchant id, so
 * concurrent ingestion requests for the *same* merchant serialize on
 * "compute MAX(sequence)+1 and insert", while different merchants never
 * block each other. The lock is released automatically at transaction end
 * (commit or rollback) -- hence Propagation.MANDATORY: this must run inside
 * the caller's transaction, never its own.
 */
@Component
public class SequenceAllocator {

    private final JdbcTemplate jdbcTemplate;

    public SequenceAllocator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long nextSequence(String merchantId) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) con -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
                ps.setString(1, merchantId);
                ps.execute();
            }
            return null;
        });

        Long max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sequence), 0) FROM events WHERE merchant_id = ?",
                Long.class, merchantId);
        return (max == null ? 0L : max) + 1;
    }
}
