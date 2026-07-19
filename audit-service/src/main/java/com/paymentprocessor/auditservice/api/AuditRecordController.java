package com.paymentprocessor.auditservice.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paymentprocessor.auditservice.api.dto.AuditRecordResponse;
import com.paymentprocessor.auditservice.api.dto.CreateAuditRecordRequest;
import com.paymentprocessor.auditservice.domain.AuditRecord;
import com.paymentprocessor.auditservice.service.AppendOutcome;
import com.paymentprocessor.auditservice.service.AuditIngestionService;
import com.paymentprocessor.auditservice.service.AuditQueryService;
import com.paymentprocessor.auditservice.service.exception.InvalidAuditEventException;

import jakarta.validation.Valid;

/**
 * Append-only audit records API.
 *
 * <p>By design this controller exposes only create (append) and read operations. There
 * is intentionally no PUT or DELETE: audit records are immutable and retained for
 * 7–10 years, so mutation and deletion are not part of the contract.
 */
@RestController
@RequestMapping("/api/v1/audit-records")
public class AuditRecordController {

    private final AuditIngestionService ingestion;
    private final AuditQueryService query;

    public AuditRecordController(AuditIngestionService ingestion, AuditQueryService query) {
        this.ingestion = ingestion;
        this.query = query;
    }

    /**
     * Appends a record. Idempotent on {@code eventId}: a repeated event returns the
     * existing record with 200, while a new event is created with 201.
     */
    @PostMapping
    public ResponseEntity<AuditRecordResponse> append(
            @Valid @RequestBody CreateAuditRecordRequest request) {
        AppendOutcome outcome = ingestion.append(request.toCommand());
        AuditRecordResponse body = AuditRecordResponse.from(outcome.record());

        if (!outcome.created()) {
            return ResponseEntity.ok(body); // idempotent replay
        }
        return ResponseEntity
                .created(URI.create("/api/v1/audit-records/" + outcome.record().getId()))
                .body(body);
    }

    @GetMapping("/{id}")
    public AuditRecordResponse getById(@PathVariable String id) {
        return AuditRecordResponse.from(query.getById(id));
    }

    @GetMapping
    public List<AuditRecordResponse> search(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "50") int limit) {

        List<AuditRecord> results;
        if (resourceType != null && resourceId != null) {
            results = query.byResource(resourceType, resourceId, limit);
        } else if (merchantId != null) {
            results = query.byMerchant(merchantId, limit);
        } else if (action != null) {
            results = query.byAction(action, limit);
        } else {
            throw new InvalidAuditEventException(
                    "Provide one of: merchantId, (resourceType + resourceId), or action");
        }
        return results.stream().map(AuditRecordResponse::from).toList();
    }
}
