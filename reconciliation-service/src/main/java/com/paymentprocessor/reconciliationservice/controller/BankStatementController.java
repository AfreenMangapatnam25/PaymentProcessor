package com.paymentprocessor.reconciliationservice.controller;

import com.paymentprocessor.reconciliationservice.dto.BankStatementResponse;
import com.paymentprocessor.reconciliationservice.dto.ReconMapper;
import com.paymentprocessor.reconciliationservice.dto.StatementIngestRequest;
import com.paymentprocessor.reconciliationservice.service.BankStatementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** REST API for ingesting and querying external bank/gateway statements. */
@RestController
@RequestMapping("/api/v1/statements")
public class BankStatementController {

    private final BankStatementService bankStatementService;

    public BankStatementController(BankStatementService bankStatementService) {
        this.bankStatementService = bankStatementService;
    }

    /** Ingest a CSV statement and attach its rows as EXTERNAL records to the given run. */
    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    public BankStatementResponse ingest(@RequestParam Long runId,
                                        @Valid @RequestBody StatementIngestRequest request) {
        return BankStatementResponse.from(
                bankStatementService.ingestCsv(runId, ReconMapper.toStatement(request), request.csvContent()));
    }

    @GetMapping("/{id}")
    public BankStatementResponse get(@PathVariable Long id) {
        return BankStatementResponse.from(bankStatementService.getById(id));
    }

    @GetMapping
    public Page<BankStatementResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return bankStatementService.list(pageable).map(BankStatementResponse::from);
    }
}
