package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.common.enums.PaymentMethodType;
import com.paymentprocessor.merchantservice.dto.PaymentMethodRequest;
import com.paymentprocessor.merchantservice.dto.PaymentMethodResponse;
import com.paymentprocessor.merchantservice.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Payment Methods")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService service;

    public PaymentMethodController(PaymentMethodService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentMethodResponse> list(@PathVariable UUID merchantId) {
        return service.list(merchantId);
    }

    @PutMapping
    public PaymentMethodResponse upsert(@PathVariable UUID merchantId,
                                        @Valid @RequestBody PaymentMethodRequest req) {
        return service.upsert(merchantId, req);
    }

    @PostMapping("/{methodType}/enable")
    public PaymentMethodResponse enable(@PathVariable UUID merchantId, @PathVariable PaymentMethodType methodType) {
        return service.setEnabled(merchantId, methodType, true);
    }

    @PostMapping("/{methodType}/disable")
    public PaymentMethodResponse disable(@PathVariable UUID merchantId, @PathVariable PaymentMethodType methodType) {
        return service.setEnabled(merchantId, methodType, false);
    }
}
