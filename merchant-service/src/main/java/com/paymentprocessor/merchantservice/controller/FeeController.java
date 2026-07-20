package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.dto.FeeConfigurationRequest;
import com.paymentprocessor.merchantservice.dto.FeeConfigurationResponse;
import com.paymentprocessor.merchantservice.dto.FeePreviewRequest;
import com.paymentprocessor.merchantservice.dto.FeePreviewResponse;
import com.paymentprocessor.merchantservice.service.FeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Fees & Pricing")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/fees")
public class FeeController {

    private final FeeService service;

    public FeeController(FeeService service) {
        this.service = service;
    }

    @GetMapping
    public FeeConfigurationResponse get(@PathVariable UUID merchantId) {
        return service.getActive(merchantId);
    }

    @PutMapping
    public FeeConfigurationResponse configure(@PathVariable UUID merchantId,
                                              @Valid @RequestBody FeeConfigurationRequest req) {
        return service.configure(merchantId, req);
    }

    @PostMapping("/preview")
    public FeePreviewResponse preview(@PathVariable UUID merchantId,
                                      @Valid @RequestBody FeePreviewRequest req) {
        return service.preview(merchantId, req);
    }
}
