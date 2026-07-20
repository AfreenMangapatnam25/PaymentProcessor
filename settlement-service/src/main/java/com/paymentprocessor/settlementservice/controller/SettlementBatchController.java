package com.paymentprocessor.settlementservice.controller;

import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.service.PayoutService;
import com.paymentprocessor.settlementservice.service.SettlementBatchService;
import com.paymentprocessor.settlementservice.service.initiation.InitiationService;
import com.paymentprocessor.settlementservice.service.reconciliation.ReconciliationService;
import com.paymentprocessor.settlementservice.service.reversal.ReversalService;
import com.paymentprocessor.settlementservice.web.dto.BatchResponse;
import com.paymentprocessor.settlementservice.web.dto.PayoutResponse;
import com.paymentprocessor.settlementservice.web.dto.ReverseBatchRequest;
import com.paymentprocessor.settlementservice.web.mapper.SettlementMapper;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Query and lifecycle operations on settlement batches. */
@RestController
@RequestMapping("/api/settlement-batches")
public class SettlementBatchController {

    private final SettlementBatchService batchService;
    private final InitiationService initiationService;
    private final ReconciliationService reconciliationService;
    private final ReversalService reversalService;
    private final PayoutService payoutService;
    private final SettlementMapper mapper;

    public SettlementBatchController(SettlementBatchService batchService,
                                     InitiationService initiationService,
                                     ReconciliationService reconciliationService,
                                     ReversalService reversalService,
                                     PayoutService payoutService,
                                     SettlementMapper mapper) {
        this.batchService = batchService;
        this.initiationService = initiationService;
        this.reconciliationService = reconciliationService;
        this.reversalService = reversalService;
        this.payoutService = payoutService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<BatchResponse> list(@RequestParam(required = false) String merchantId,
                                    @RequestParam(required = false) BatchStatus status) {
        List<SettlementBatch> batches;
        if (merchantId != null) {
            batches = batchService.findByMerchant(merchantId);
        } else if (status != null) {
            batches = batchService.findByStatus(status);
        } else {
            batches = batchService.findAll();
        }
        return mapper.toBatchResponses(batches);
    }

    @GetMapping("/{id}")
    public BatchResponse get(@PathVariable String id) {
        return mapper.toBatchResponse(batchService.findById(id));
    }

    @GetMapping("/{id}/payouts")
    public List<PayoutResponse> payouts(@PathVariable String id) {
        return mapper.toPayoutResponses(payoutService.findByBatch(id));
    }

    @PostMapping("/{id}/initiate")
    public BatchResponse initiate(@PathVariable String id) {
        SettlementBatch batch = batchService.findById(id);
        return mapper.toBatchResponse(initiationService.initiateBatch(batch));
    }

    @PostMapping("/{id}/reconcile")
    public BatchResponse reconcile(@PathVariable String id) {
        return mapper.toBatchResponse(reconciliationService.reconcile(id));
    }

    @PostMapping("/{id}/reverse")
    public BatchResponse reverse(@PathVariable String id, @Valid @RequestBody ReverseBatchRequest request) {
        return mapper.toBatchResponse(
                reversalService.reverse(id, request.reason(), request.approvedBy(), request.approverLevel()));
    }
}
