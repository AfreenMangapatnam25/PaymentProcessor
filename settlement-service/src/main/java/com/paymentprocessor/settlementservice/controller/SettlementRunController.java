package com.paymentprocessor.settlementservice.controller;

import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.service.SettlementRunService;
import com.paymentprocessor.settlementservice.web.dto.BatchResponse;
import com.paymentprocessor.settlementservice.web.dto.RunCycleRequest;
import com.paymentprocessor.settlementservice.web.mapper.SettlementMapper;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manually triggers a settlement run — either the full cycle across all
 * merchants with pending items, or a single merchant + currency.
 */
@RestController
@RequestMapping("/api/settlement-runs")
public class SettlementRunController {

    private final SettlementRunService runService;
    private final SettlementMapper mapper;

    public SettlementRunController(SettlementRunService runService, SettlementMapper mapper) {
        this.runService = runService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<?> run(@RequestBody(required = false) RunCycleRequest request) {
        RunCycleRequest req = request == null ? new RunCycleRequest(null, null, null) : request;

        if (req.merchantId() != null && req.currency() != null) {
            Optional<SettlementBatch> batch =
                    runService.runForMerchant(req.merchantId(), req.currency(), req.scheduleTypeOrDefault());
            return batch.<ResponseEntity<?>>map(b -> ResponseEntity.ok(mapper.toBatchResponse(b)))
                    .orElseGet(() -> ResponseEntity.noContent().build());
        }

        SettlementRunService.RunSummary summary = runService.runCycle(req.scheduleTypeOrDefault());
        return ResponseEntity.ok(summary);
    }
}
