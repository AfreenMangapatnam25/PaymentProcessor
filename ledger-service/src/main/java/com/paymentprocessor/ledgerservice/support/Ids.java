package com.paymentprocessor.ledgerservice.support;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Identifier generation for ledger aggregates. Journal ids embed the posting
 * date for human readability (e.g. {@code JE-20260719-8f3a1c2b4d5e}).
 */
public final class Ids {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private Ids() {
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String journalId(Instant postedAt) {
        return "JE-" + DAY.format(postedAt) + "-" + shortUuid();
    }

    public static String accountId() {
        return "ACC-" + shortUuid();
    }

    public static String holdId() {
        return "HLD-" + shortUuid();
    }

    public static String periodId(String code) {
        return "PERIOD-" + code;
    }

    public static String outboxId() {
        return UUID.randomUUID().toString();
    }
}
