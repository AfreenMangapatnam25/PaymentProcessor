package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.entity.AccountingPeriod;
import com.paymentprocessor.ledgerservice.service.AccountingPeriodService;
import com.paymentprocessor.ledgerservice.web.dto.CreatePeriodRequest;
import com.paymentprocessor.ledgerservice.web.dto.PeriodResponse;
import com.paymentprocessor.ledgerservice.web.dto.UpdatePeriodStateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/periods")
@Tag(name = "Accounting Periods", description = "Open, close, and lock reportable intervals")
public class PeriodController {

    private final AccountingPeriodService periodService;

    public PeriodController(AccountingPeriodService periodService) {
        this.periodService = periodService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an accounting period")
    public PeriodResponse create(@Valid @RequestBody CreatePeriodRequest request) {
        return toResponse(periodService.create(request));
    }

    @GetMapping
    @Operation(summary = "List accounting periods")
    public List<PeriodResponse> list() {
        return periodService.list().stream().map(PeriodController::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an accounting period by id")
    public PeriodResponse get(@PathVariable String id) {
        return toResponse(periodService.get(id));
    }

    @PostMapping("/{id}/state")
    @Operation(summary = "Transition a period (OPEN, CLOSING, CLOSED, LOCKED)")
    public PeriodResponse changeState(@PathVariable String id,
                                      @Valid @RequestBody UpdatePeriodStateRequest request) {
        return toResponse(periodService.changeState(id, request.state()));
    }

    private static PeriodResponse toResponse(AccountingPeriod p) {
        return new PeriodResponse(p.getId(), p.getCode(), p.getPeriodType(), p.getStartDate(),
                p.getEndDate(), p.getState(), p.getCreatedAt(), p.getClosedAt());
    }
}
