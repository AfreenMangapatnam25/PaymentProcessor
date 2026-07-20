package com.paymentprocessor.reconciliationservice.controller;

import com.paymentprocessor.reconciliationservice.domain.ExceptionStatus;
import com.paymentprocessor.reconciliationservice.domain.ReviewQueue;
import com.paymentprocessor.reconciliationservice.domain.SeverityLevel;
import com.paymentprocessor.reconciliationservice.dto.*;
import com.paymentprocessor.reconciliationservice.service.AdjustmentService;
import com.paymentprocessor.reconciliationservice.service.ExceptionRecordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** REST API for the manual-reconciliation workflow over exceptions and their adjustments. */
@RestController
@RequestMapping("/api/v1/exceptions")
public class ExceptionRecordController {

    private final ExceptionRecordService exceptionRecordService;
    private final AdjustmentService adjustmentService;

    public ExceptionRecordController(ExceptionRecordService exceptionRecordService,
                                     AdjustmentService adjustmentService) {
        this.exceptionRecordService = exceptionRecordService;
        this.adjustmentService = adjustmentService;
    }

    @GetMapping
    public Page<ExceptionResponse> list(@RequestParam(required = false) ExceptionStatus status,
                                        @RequestParam(required = false) SeverityLevel severity,
                                        @RequestParam(required = false) ReviewQueue queue,
                                        @RequestParam(required = false) Long runId,
                                        @PageableDefault(size = 50) Pageable pageable) {
        return exceptionRecordService.list(status, severity, queue, runId, pageable)
                .map(ExceptionResponse::from);
    }

    @GetMapping("/{id}")
    public ExceptionResponse get(@PathVariable Long id) {
        return ExceptionResponse.from(exceptionRecordService.getById(id));
    }

    @GetMapping("/uuid/{uuid}")
    public ExceptionResponse getByUuid(@PathVariable UUID uuid) {
        return ExceptionResponse.from(exceptionRecordService.getByUuid(uuid));
    }

    @PostMapping("/{id}/assign")
    public ExceptionResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return ExceptionResponse.from(exceptionRecordService.assign(id, request.assignee()));
    }

    @PostMapping("/{id}/resolve")
    public ExceptionResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        return ExceptionResponse.from(exceptionRecordService.resolve(
                id, request.resolutionType(), request.note(), request.resolvedBy()));
    }

    @PostMapping("/{id}/escalate")
    public ExceptionResponse escalate(@PathVariable Long id, @Valid @RequestBody EscalateRequest request) {
        return ExceptionResponse.from(exceptionRecordService.escalate(
                id, request.queue(), request.note(), request.actor()));
    }

    @PostMapping("/{id}/defer")
    public ExceptionResponse defer(@PathVariable Long id, @Valid @RequestBody ActorNoteRequest request) {
        return ExceptionResponse.from(exceptionRecordService.defer(id, request.note(), request.actor()));
    }

    @PostMapping("/{id}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public AdjustmentResponse createAdjustment(@PathVariable Long id,
                                               @Valid @RequestBody AdjustmentCreateRequest request) {
        return AdjustmentResponse.from(
                adjustmentService.create(id, ReconMapper.toAdjustment(request)));
    }

    @GetMapping("/{id}/adjustments")
    public List<AdjustmentResponse> listAdjustments(@PathVariable Long id) {
        return adjustmentService.listByException(id).stream().map(AdjustmentResponse::from).toList();
    }
}
