package com.paymentprocessor.settlementservice.controller;

import com.paymentprocessor.settlementservice.entity.PayoutReturn;
import com.paymentprocessor.settlementservice.service.PayoutReturnService;
import com.paymentprocessor.settlementservice.service.PayoutService;
import com.paymentprocessor.settlementservice.service.initiation.InitiationService;
import com.paymentprocessor.settlementservice.web.dto.PayoutResponse;
import com.paymentprocessor.settlementservice.web.dto.RecordReturnRequest;
import com.paymentprocessor.settlementservice.web.mapper.SettlementMapper;
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
import org.springframework.web.bind.annotation.RestController;

/** Query payouts and drive confirmation / return handling. */
@RestController
@RequestMapping("/api/payouts")
public class PayoutController {

    private final PayoutService payoutService;
    private final InitiationService initiationService;
    private final PayoutReturnService payoutReturnService;
    private final SettlementMapper mapper;

    public PayoutController(PayoutService payoutService,
                            InitiationService initiationService,
                            PayoutReturnService payoutReturnService,
                            SettlementMapper mapper) {
        this.payoutService = payoutService;
        this.initiationService = initiationService;
        this.payoutReturnService = payoutReturnService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<PayoutResponse> list(@RequestParam(required = false) String merchantId,
                                     @RequestParam(required = false) String batchId) {
        if (merchantId != null) {
            return mapper.toPayoutResponses(payoutService.findByMerchant(merchantId));
        }
        if (batchId != null) {
            return mapper.toPayoutResponses(payoutService.findByBatch(batchId));
        }
        return mapper.toPayoutResponses(payoutService.findAll());
    }

    @GetMapping("/{id}")
    public PayoutResponse get(@PathVariable String id) {
        return mapper.toPayoutResponse(payoutService.findById(id));
    }

    /** Simulated bank confirmation that a processing payout settled. */
    @PostMapping("/{id}/confirm")
    public PayoutResponse confirm(@PathVariable String id) {
        return mapper.toPayoutResponse(initiationService.confirmPayout(id));
    }

    /** Record a bank return against a payout. */
    @PostMapping("/{id}/returns")
    public ResponseEntity<PayoutReturn> recordReturn(@PathVariable String id,
                                                     @Valid @RequestBody RecordReturnRequest request) {
        PayoutReturn ret = payoutReturnService.recordReturn(id, request.reasonCode(), request.reasonDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(ret);
    }
}
