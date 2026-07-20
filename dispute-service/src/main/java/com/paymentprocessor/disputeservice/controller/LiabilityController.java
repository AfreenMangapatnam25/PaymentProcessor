package com.paymentprocessor.disputeservice.controller;

import com.paymentprocessor.disputeservice.dto.response.LiabilityResponse;
import com.paymentprocessor.disputeservice.exception.DisputeNotFoundException;
import com.paymentprocessor.disputeservice.service.LiabilityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only REST API exposing the financial impact of a dispute.
 */
@RestController
@RequestMapping("/api/v1/disputes/{disputeId}")
public class LiabilityController {

    private final LiabilityService liabilityService;

    public LiabilityController(LiabilityService liabilityService) {
        this.liabilityService = liabilityService;
    }

    /** The current (latest) financial impact record for a dispute. */
    @GetMapping("/liability")
    public LiabilityResponse current(@PathVariable String disputeId) {
        return liabilityService.current(disputeId)
                .map(LiabilityResponse::from)
                .orElseThrow(() -> new DisputeNotFoundException(
                        "No liability recorded for dispute " + disputeId));
    }

    /** The full history of financial impact records for a dispute. */
    @GetMapping("/liability/history")
    public List<LiabilityResponse> history(@PathVariable String disputeId) {
        return liabilityService.history(disputeId).stream().map(LiabilityResponse::from).toList();
    }
}
