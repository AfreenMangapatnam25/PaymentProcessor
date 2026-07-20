package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.common.enums.KycStatus;
import com.paymentprocessor.merchantservice.dto.BeneficialOwnerRequest;
import com.paymentprocessor.merchantservice.dto.BeneficialOwnerResponse;
import com.paymentprocessor.merchantservice.service.BeneficialOwnerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Beneficial Owners")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/beneficial-owners")
public class BeneficialOwnerController {

    private final BeneficialOwnerService service;

    public BeneficialOwnerController(BeneficialOwnerService service) {
        this.service = service;
    }

    @GetMapping
    public List<BeneficialOwnerResponse> list(@PathVariable UUID merchantId) {
        return service.list(merchantId);
    }

    @PostMapping
    public ResponseEntity<BeneficialOwnerResponse> add(@PathVariable UUID merchantId,
                                                       @Valid @RequestBody BeneficialOwnerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(merchantId, req));
    }

    @PutMapping("/{ownerId}")
    public BeneficialOwnerResponse update(@PathVariable UUID merchantId, @PathVariable UUID ownerId,
                                          @Valid @RequestBody BeneficialOwnerRequest req) {
        return service.update(merchantId, ownerId, req);
    }

    @PutMapping("/{ownerId}/kyc-status")
    public BeneficialOwnerResponse updateKyc(@PathVariable UUID merchantId, @PathVariable UUID ownerId,
                                             @RequestParam @NotNull KycStatus status) {
        return service.updateKycStatus(merchantId, ownerId, status);
    }

    @DeleteMapping("/{ownerId}")
    public ResponseEntity<Void> delete(@PathVariable UUID merchantId, @PathVariable UUID ownerId) {
        service.delete(merchantId, ownerId);
        return ResponseEntity.noContent().build();
    }
}
