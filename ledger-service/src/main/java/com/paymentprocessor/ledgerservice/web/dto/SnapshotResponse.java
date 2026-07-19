package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.SnapshotType;
import java.time.Instant;
import java.time.LocalDate;

public record SnapshotResponse(
        String accountId,
        LocalDate asOfDate,
        SnapshotType snapshotType,
        long openingMinor,
        long debitMinor,
        long creditMinor,
        long closingMinor,
        long entryCount,
        long entryHighWater,
        Instant lastEntryAt,
        Instant createdAt
) {
}
