package com.paymentprocessor.notificationservice.service;

import com.paymentprocessor.notificationservice.config.RetentionProperties;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Partition maintenance for events and webhook_deliveries (both RANGE-
 * partitioned by created_at, monthly). Two jobs:
 *
 *  - createUpcomingPartitions: keeps real monthly partitions ahead of the
 *    write path so inserts never fall through to the DEFAULT partition
 *    (which isn't indexed as well and isn't part of the retention scheme).
 *  - dropExpiredPartitions: enforces the 90-day retention window by
 *    dropping whole partitions once every row in them is older than the
 *    cutoff -- a metadata-only operation, unlike a row-by-row DELETE of
 *    potentially millions of rows.
 *
 * Both operate purely on the events_YYYY_MM / webhook_deliveries_YYYY_MM
 * naming convention this service controls (see V1__init_schema.sql), rather
 * than introspecting pg_inherits/pg_class, since it owns that convention
 * end to end.
 */
@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);
    private static final DateTimeFormatter SUFFIX = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final String[] PARTITIONED_TABLES = {"events", "webhook_deliveries"};

    private final JdbcTemplate jdbcTemplate;
    private final RetentionProperties properties;

    public RetentionService(JdbcTemplate jdbcTemplate, RetentionProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Scheduled(cron = "0 30 0 * * *") // daily at 00:30
    public void runMaintenance() {
        createUpcomingPartitions();
        dropExpiredPartitions();
    }

    public void createUpcomingPartitions() {
        YearMonth current = YearMonth.now();
        for (int i = 0; i <= properties.getMonthsAheadToCreate(); i++) {
            createMonthPartitions(current.plusMonths(i));
        }
    }

    public void dropExpiredPartitions() {
        LocalDate cutoff = LocalDate.now().minusDays(properties.getRetentionDays());
        YearMonth cutoffMonth = YearMonth.from(cutoff);
        // Sweep a generous look-back window; DROP TABLE IF EXISTS makes checking
        // months that were never created (or already dropped) a harmless no-op.
        for (int i = 1; i <= 36; i++) {
            YearMonth candidate = cutoffMonth.minusMonths(i);
            // Only months that end at or before the cutoff are eligible.
            if (!candidate.plusMonths(1).atDay(1).isAfter(cutoff)) {
                dropMonthPartitions(candidate);
            }
        }
    }

    private void createMonthPartitions(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        String suffix = SUFFIX.format(month.atDay(1));
        for (String table : PARTITIONED_TABLES) {
            String partitionName = table + "_" + suffix;
            jdbcTemplate.execute(String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s')",
                    partitionName, table, start, end));
        }
    }

    private void dropMonthPartitions(YearMonth month) {
        String suffix = SUFFIX.format(month.atDay(1));
        for (String table : PARTITIONED_TABLES) {
            String partitionName = table + "_" + suffix;
            int before = countTable(partitionName);
            if (before == 0) {
                continue;
            }
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + partitionName);
            log.info("dropped expired partition {} (retention: {} days)", partitionName, properties.getRetentionDays());
        }
    }

    private int countTable(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", Integer.class, tableName);
        return count == null ? 0 : count;
    }
}
