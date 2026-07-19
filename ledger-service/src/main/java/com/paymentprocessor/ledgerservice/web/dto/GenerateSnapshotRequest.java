package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.SnapshotType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Request to generate balance snapshots as of a date. When {@code accountIds}
 * is null or empty, snapshots are generated for all accounts.
 */
public record GenerateSnapshotRequest(
        @NotNull LocalDate asOfDate,
        SnapshotType snapshotType,
        List<String> accountIds
) {
}
