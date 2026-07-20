package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.dto.BrandingRequest;
import com.paymentprocessor.merchantservice.dto.BrandingResponse;
import com.paymentprocessor.merchantservice.service.BrandingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Branding")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/branding")
public class BrandingController {

    private final BrandingService service;

    public BrandingController(BrandingService service) {
        this.service = service;
    }

    @GetMapping
    public BrandingResponse get(@PathVariable UUID merchantId) {
        return service.get(merchantId);
    }

    @PutMapping
    public BrandingResponse update(@PathVariable UUID merchantId,
                                   @Valid @RequestBody BrandingRequest req) {
        return service.update(merchantId, req);
    }
}
