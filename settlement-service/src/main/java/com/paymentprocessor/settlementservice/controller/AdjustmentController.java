package com.paymentprocessor.settlementservice.controller;

import com.paymentprocessor.settlementservice.entity.Adjustment;
import com.paymentprocessor.settlementservice.service.AdjustmentService;
import com.paymentprocessor.settlementservice.service.IdempotencyService;
import com.paymentprocessor.settlementservice.web.dto.AdjustmentResponse;
import com.paymentprocessor.settlementservice.web.dto.ApproveAdjustmentRequest;
import com.paymentprocessor.settlementservice.web.dto.CreateAdjustmentRequest;
import com.paymentprocessor.settlementservice.web.mapper.SettlementMapper;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Create, approve, reject, and query settlement adjustments. */
@RestController
@RequestMapping("/api/adjustments")
public class AdjustmentController {

    private final AdjustmentService service;
    private final IdempotencyService idempotencyService;
    private final SettlementMapper mapper;

    public AdjustmentController(AdjustmentService service,
                               IdempotencyService idempotencyService,
                               SettlementMapper mapper) {
        this.service = service;
        this.idempotencyService = idempotencyService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AdjustmentResponse> create(
            @Valid @RequestBody CreateAdjustmentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AdjustmentResponse response = idempotencyService.execute(idempotencyKey, "Adjustment",
                AdjustmentResponse.class, () -> {
                    Adjustment adjustment = new Adjustment();
                    adjustment.setMerchantId(request.merchantId());
                    adjustment.setType(request.type());
                    adjustment.setAmountMinor(request.amountMinor());
                    adjustment.setCurrency(request.currency());
                    adjustment.setReasonCode(request.reasonCode());
                    adjustment.setDescription(request.description());
                    adjustment.setRequestedBy(request.requestedBy());
                    return mapper.toAdjustmentResponse(service.request(adjustment));
                });
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/approve")
    public AdjustmentResponse approve(@PathVariable String id,
                                      @Valid @RequestBody ApproveAdjustmentRequest request) {
        return mapper.toAdjustmentResponse(
                service.approve(id, request.approvedBy(), request.approverLevel()));
    }

    @PostMapping("/{id}/reject")
    public AdjustmentResponse reject(@PathVariable String id, @RequestParam String rejectedBy) {
        return mapper.toAdjustmentResponse(service.reject(id, rejectedBy));
    }

    @GetMapping("/{id}")
    public AdjustmentResponse get(@PathVariable String id) {
        return mapper.toAdjustmentResponse(service.getOrThrow(id));
    }

    @GetMapping
    public List<AdjustmentResponse> list(@RequestParam(required = false) String merchantId) {
        return mapper.toAdjustmentResponses(
                merchantId != null ? service.findByMerchant(merchantId) : service.findAll());
    }
}
