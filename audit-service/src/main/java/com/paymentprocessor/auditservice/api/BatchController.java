package com.paymentprocessor.auditservice.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentprocessor.auditservice.api.dto.BatchResponse;
import com.paymentprocessor.auditservice.batch.DailyBatchService;
import com.paymentprocessor.auditservice.domain.AuditBatch;
import com.paymentprocessor.auditservice.repository.AuditBatchRepository;
import com.paymentprocessor.auditservice.service.exception.ResourceNotFoundException;

/**
 * Administrative endpoints for the legal-copy daily batches. Sealing normally runs on a
 * schedule; the manual seal endpoint supports backfills and operational recovery.
 */
@RestController
@RequestMapping("/api/v1/audit/batches")
public class BatchController {

    private final DailyBatchService batchService;
    private final AuditBatchRepository batches;

    public BatchController(DailyBatchService batchService, AuditBatchRepository batches) {
        this.batchService = batchService;
        this.batches = batches;
    }

    /** Returns the manifest for a given UTC day, e.g. {@code /2026-07-16}. */
    @GetMapping("/{date}")
    public BatchResponse get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuditBatch batch = batches.findByBatchDate(date)
                .orElseThrow(() -> new ResourceNotFoundException("No batch for date " + date));
        return BatchResponse.from(batch);
    }

    /**
     * Manually seals a given UTC day (idempotent). Useful for backfilling or completing a
     * batch that failed mid-way.
     */
    @PostMapping("/{date}/seal")
    public ResponseEntity<BatchResponse> seal(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuditBatch batch = batchService.sealDay(date);
        return ResponseEntity.ok(BatchResponse.from(batch));
    }
}
