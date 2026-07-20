package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.dto.MerchantConfigurationRequest;
import com.paymentprocessor.merchantservice.dto.MerchantConfigurationResponse;
import com.paymentprocessor.merchantservice.service.MerchantConfigurationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Merchant Configuration")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/configuration")
public class MerchantConfigurationController {

    private final MerchantConfigurationService service;

    public MerchantConfigurationController(MerchantConfigurationService service) {
        this.service = service;
    }

    @GetMapping
    public MerchantConfigurationResponse get(@PathVariable UUID merchantId) {
        return service.get(merchantId);
    }

    @PatchMapping
    public MerchantConfigurationResponse update(@PathVariable UUID merchantId,
                                                @Valid @RequestBody MerchantConfigurationRequest req) {
        return service.update(merchantId, req);
    }
}
