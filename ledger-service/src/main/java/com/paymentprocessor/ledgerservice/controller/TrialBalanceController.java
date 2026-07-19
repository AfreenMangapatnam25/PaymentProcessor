package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.service.TrialBalanceService;
import com.paymentprocessor.ledgerservice.web.dto.TrialBalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trial-balance")
@Tag(name = "Trial Balance", description = "Verify that total debits equal total credits")
public class TrialBalanceController {

    private final TrialBalanceService trialBalanceService;

    public TrialBalanceController(TrialBalanceService trialBalanceService) {
        this.trialBalanceService = trialBalanceService;
    }

    @GetMapping
    @Operation(summary = "Generate a trial balance as of an instant (defaults to now)")
    public TrialBalanceResponse generate(@RequestParam(required = false) Instant asOf) {
        return trialBalanceService.generate(asOf);
    }
}
