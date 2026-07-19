package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;
import com.paymentprocessor.merchantservice.dto.MerchantOnboardingRequest;
import com.paymentprocessor.merchantservice.dto.MerchantProfilePatchRequest;
import com.paymentprocessor.merchantservice.dto.MerchantProfileUpdateRequest;
import com.paymentprocessor.merchantservice.dto.MerchantResponse;
import com.paymentprocessor.merchantservice.dto.PageResponse;
import com.paymentprocessor.merchantservice.dto.PricingPlanAssignmentRequest;
import com.paymentprocessor.merchantservice.dto.StatusChangeRequest;
import com.paymentprocessor.merchantservice.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Merchants", description = "Merchant onboarding, profile, and lifecycle")
@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @Operation(summary = "Onboard a new merchant (admin)")
    @PostMapping
    public ResponseEntity<MerchantResponse> onboard(@Valid @RequestBody MerchantOnboardingRequest req,
                                                    UriComponentsBuilder uriBuilder) {
        MerchantResponse created = merchantService.onboard(req);
        URI location = uriBuilder.path("/api/v1/merchants/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List merchants (admin)")
    @GetMapping
    public PageResponse<MerchantResponse> list(@RequestParam(required = false) MerchantStatus status,
                                               Pageable pageable) {
        return PageResponse.from(merchantService.list(status, pageable));
    }

    @Operation(summary = "Get a merchant by id")
    @GetMapping("/{merchantId}")
    public MerchantResponse get(@PathVariable UUID merchantId) {
        return merchantService.get(merchantId);
    }

    @Operation(summary = "Replace merchant profile")
    @PutMapping("/{merchantId}")
    public MerchantResponse update(@PathVariable UUID merchantId,
                                   @Valid @RequestBody MerchantProfileUpdateRequest req) {
        return merchantService.updateProfile(merchantId, req);
    }

    @Operation(summary = "Partially update merchant profile")
    @PatchMapping("/{merchantId}")
    public MerchantResponse patch(@PathVariable UUID merchantId,
                                  @Valid @RequestBody MerchantProfilePatchRequest req) {
        return merchantService.patchProfile(merchantId, req);
    }

    @Operation(summary = "Change merchant lifecycle status (admin)")
    @PostMapping("/{merchantId}/status")
    public MerchantResponse changeStatus(@PathVariable UUID merchantId,
                                         @Valid @RequestBody StatusChangeRequest req) {
        return merchantService.changeStatus(merchantId, req);
    }

    @Operation(summary = "Assign a pricing plan (admin)")
    @PutMapping("/{merchantId}/pricing-plan")
    public MerchantResponse assignPricingPlan(@PathVariable UUID merchantId,
                                              @Valid @RequestBody PricingPlanAssignmentRequest req) {
        return merchantService.assignPricingPlan(merchantId, req.pricingPlan());
    }
}
