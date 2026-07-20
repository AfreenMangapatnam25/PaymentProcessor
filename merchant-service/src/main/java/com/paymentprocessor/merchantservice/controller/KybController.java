package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.dto.KybCaseResponse;
import com.paymentprocessor.merchantservice.dto.KybDecisionRequest;
import com.paymentprocessor.merchantservice.dto.KybDocumentRequest;
import com.paymentprocessor.merchantservice.dto.KybDocumentResponse;
import com.paymentprocessor.merchantservice.service.KybService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "KYB Verification")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/kyb")
public class KybController {

    private final KybService service;

    public KybController(KybService service) {
        this.service = service;
    }

    @GetMapping("/cases")
    public List<KybCaseResponse> listCases(@PathVariable UUID merchantId) {
        return service.listCases(merchantId);
    }

    @GetMapping("/cases/latest")
    public KybCaseResponse latest(@PathVariable UUID merchantId) {
        return service.latestCase(merchantId);
    }

    @PostMapping("/cases")
    public ResponseEntity<KybCaseResponse> submit(@PathVariable UUID merchantId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submitCase(merchantId));
    }

    @PostMapping("/cases/{caseId}/decision")
    public KybCaseResponse decide(@PathVariable UUID merchantId, @PathVariable UUID caseId,
                                  @Valid @RequestBody KybDecisionRequest req) {
        return service.applyDecision(merchantId, caseId, req);
    }

    @GetMapping("/documents")
    public List<KybDocumentResponse> listDocuments(@PathVariable UUID merchantId) {
        return service.listDocuments(merchantId);
    }

    @PostMapping("/documents")
    public ResponseEntity<KybDocumentResponse> addDocument(@PathVariable UUID merchantId,
                                                           @Valid @RequestBody KybDocumentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addDocument(merchantId, req));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID merchantId, @PathVariable UUID documentId) {
        service.deleteDocument(merchantId, documentId);
        return ResponseEntity.noContent().build();
    }
}
