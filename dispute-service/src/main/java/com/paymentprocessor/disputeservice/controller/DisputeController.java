package com.paymentprocessor.disputeservice.controller;

import com.paymentprocessor.disputeservice.dto.request.CreateDisputeRequest;
import com.paymentprocessor.disputeservice.dto.response.DisputeDetailResponse;
import com.paymentprocessor.disputeservice.dto.response.DisputeResponse;
import com.paymentprocessor.disputeservice.dto.response.EvidenceResponse;
import com.paymentprocessor.disputeservice.dto.response.LiabilityResponse;
import com.paymentprocessor.disputeservice.dto.response.RepresentmentResponse;
import com.paymentprocessor.disputeservice.dto.response.TimelineEventResponse;
import com.paymentprocessor.disputeservice.entity.Dispute;
import com.paymentprocessor.disputeservice.service.DisputeEventService;
import com.paymentprocessor.disputeservice.service.DisputeService;
import com.paymentprocessor.disputeservice.service.EvidenceService;
import com.paymentprocessor.disputeservice.service.LiabilityService;
import com.paymentprocessor.disputeservice.service.RepresentmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the dispute lifecycle: creation, retrieval, status and the
 * lifecycle actions (request evidence, accept liability, close).
 */
@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeController {

    private final DisputeService disputeService;
    private final EvidenceService evidenceService;
    private final RepresentmentService representmentService;
    private final LiabilityService liabilityService;
    private final DisputeEventService eventService;

    public DisputeController(DisputeService disputeService, EvidenceService evidenceService,
                             RepresentmentService representmentService,
                             LiabilityService liabilityService, DisputeEventService eventService) {
        this.disputeService = disputeService;
        this.evidenceService = evidenceService;
        this.representmentService = representmentService;
        this.liabilityService = liabilityService;
        this.eventService = eventService;
    }

    /** Opens a new dispute from an inbound chargeback / retrieval notification. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeResponse create(@Valid @RequestBody CreateDisputeRequest request) {
        return DisputeResponse.from(disputeService.createFromChargeback(request));
    }

    /** Lists disputes, optionally filtered by merchant. */
    @GetMapping
    public List<DisputeResponse> list(@RequestParam(required = false) String merchantId) {
        List<Dispute> disputes = merchantId != null
                ? disputeService.findByMerchant(merchantId)
                : disputeService.findAll();
        return disputes.stream().map(DisputeResponse::from).toList();
    }

    /** Full dispute detail: evidence, representments, liability and timeline. */
    @GetMapping("/{id}")
    public DisputeDetailResponse get(@PathVariable String id) {
        DisputeResponse dispute = DisputeResponse.from(disputeService.get(id));
        LiabilityResponse liability = liabilityService.current(id)
                .map(LiabilityResponse::from).orElse(null);
        List<EvidenceResponse> evidence = evidenceService.forDispute(id).stream()
                .map(EvidenceResponse::from).toList();
        List<RepresentmentResponse> representments = representmentService.forDispute(id).stream()
                .map(RepresentmentResponse::from).toList();
        List<TimelineEventResponse> timeline = eventService.getTimeline(id).stream()
                .map(TimelineEventResponse::from).toList();
        return new DisputeDetailResponse(dispute, liability, evidence, representments, timeline);
    }

    /** Current status only. */
    @GetMapping("/{id}/status")
    public DisputeResponse status(@PathVariable String id) {
        return DisputeResponse.from(disputeService.get(id));
    }

    /** Requests evidence from the merchant (OPEN -> PENDING_EVIDENCE). */
    @PostMapping("/{id}/request-evidence")
    public DisputeResponse requestEvidence(@PathVariable String id) {
        return DisputeResponse.from(disputeService.requestEvidence(id));
    }

    /** Moves the dispute into review once evidence is supplied. */
    @PostMapping("/{id}/review")
    public DisputeResponse markUnderReview(@PathVariable String id) {
        return DisputeResponse.from(disputeService.markEvidenceUnderReview(id));
    }

    /** Merchant / platform accepts liability without fighting. */
    @PostMapping("/{id}/accept")
    public DisputeResponse accept(@PathVariable String id,
                                  @RequestParam(required = false) String actor) {
        return DisputeResponse.from(disputeService.acceptLiability(id, actor));
    }

    /** Closes a resolved dispute. */
    @PostMapping("/{id}/close")
    public DisputeResponse close(@PathVariable String id) {
        return DisputeResponse.from(disputeService.close(id));
    }
}
