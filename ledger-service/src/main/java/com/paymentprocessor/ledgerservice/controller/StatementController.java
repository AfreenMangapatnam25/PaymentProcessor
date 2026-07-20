package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.service.StatementService;
import com.paymentprocessor.ledgerservice.web.dto.StatementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Statements", description = "Historical account statements with running balances")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping("/api/v1/accounts/{accountId}/statement")
    @Operation(summary = "Get an account statement for a time window (from exclusive, to inclusive)")
    public StatementResponse statement(@PathVariable String accountId,
                                       @RequestParam(required = false) Instant from,
                                       @RequestParam(required = false) Instant to) {
        return statementService.statement(accountId, from, to);
    }
}
