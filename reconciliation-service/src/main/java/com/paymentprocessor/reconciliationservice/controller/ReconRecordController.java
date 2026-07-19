package com.paymentprocessor.reconciliationservice.controller;

import com.paymentprocessor.reconciliationservice.dto.ReconRecordResponse;
import com.paymentprocessor.reconciliationservice.service.ReconRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read access to individual reconciliation records. */
@RestController
@RequestMapping("/api/v1/records")
public class ReconRecordController {

    private final ReconRecordService reconRecordService;

    public ReconRecordController(ReconRecordService reconRecordService) {
        this.reconRecordService = reconRecordService;
    }

    @GetMapping("/{id}")
    public ReconRecordResponse get(@PathVariable Long id) {
        return ReconRecordResponse.from(reconRecordService.getRecord(id));
    }
}
