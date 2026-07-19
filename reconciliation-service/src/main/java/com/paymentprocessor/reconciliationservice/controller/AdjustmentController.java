package com.paymentprocessor.reconciliationservice.controller;

import com.paymentprocessor.reconciliationservice.dto.ActorRequest;
import com.paymentprocessor.reconciliationservice.dto.AdjustmentResponse;
import com.paymentprocessor.reconciliationservice.service.AdjustmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** REST API for approving, posting and rejecting adjustments. */
@RestController
@RequestMapping("/api/v1/adjustments")
public class AdjustmentController {

    private final AdjustmentService adjustmentService;

    public AdjustmentController(AdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @GetMapping("/{id}")
    public AdjustmentResponse get(@PathVariable Long id) {
        return AdjustmentResponse.from(adjustmentService.getById(id));
    }

    @PostMapping("/{id}/approve")
    public AdjustmentResponse approve(@PathVariable Long id, @Valid @RequestBody ActorRequest request) {
        return AdjustmentResponse.from(adjustmentService.approve(id, request.actor()));
    }

    @PostMapping("/{id}/post")
    public AdjustmentResponse post(@PathVariable Long id, @Valid @RequestBody ActorRequest request) {
        return AdjustmentResponse.from(adjustmentService.post(id, request.actor()));
    }

    @PostMapping("/{id}/reject")
    public AdjustmentResponse reject(@PathVariable Long id, @Valid @RequestBody ActorRequest request) {
        return AdjustmentResponse.from(adjustmentService.reject(id, request.actor()));
    }
}
