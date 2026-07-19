package com.paymentprocessor.disputeservice.controller;

import com.paymentprocessor.disputeservice.dto.request.ReviewEvidenceRequest;
import com.paymentprocessor.disputeservice.dto.request.UploadEvidenceRequest;
import com.paymentprocessor.disputeservice.dto.response.EvidenceResponse;
import com.paymentprocessor.disputeservice.service.EvidenceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for evidence upload and review.
 */
@RestController
@RequestMapping("/api/v1")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    /** Registers an uploaded evidence document against a dispute. */
    @PostMapping("/disputes/{disputeId}/evidence")
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenceResponse upload(@PathVariable String disputeId,
                                   @Valid @RequestBody UploadEvidenceRequest request) {
        return EvidenceResponse.from(evidenceService.upload(disputeId, request));
    }

    /** Lists all evidence for a dispute. */
    @GetMapping("/disputes/{disputeId}/evidence")
    public List<EvidenceResponse> list(@PathVariable String disputeId) {
        return evidenceService.forDispute(disputeId).stream().map(EvidenceResponse::from).toList();
    }

    /** Retrieves a single evidence document. */
    @GetMapping("/evidence/{evidenceId}")
    public EvidenceResponse get(@PathVariable String evidenceId) {
        return EvidenceResponse.from(evidenceService.get(evidenceId));
    }

    /** Records a review decision (accept / reject) on a document. */
    @PostMapping("/evidence/{evidenceId}/review")
    public EvidenceResponse review(@PathVariable String evidenceId,
                                   @Valid @RequestBody ReviewEvidenceRequest request) {
        return EvidenceResponse.from(evidenceService.review(evidenceId, request));
    }
}
