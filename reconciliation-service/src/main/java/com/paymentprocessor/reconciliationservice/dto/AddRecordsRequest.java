package com.paymentprocessor.reconciliationservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Batch of records to ingest into a run. */
public record AddRecordsRequest(
        @NotEmpty @Valid List<ReconRecordRequest> records
) {
}
