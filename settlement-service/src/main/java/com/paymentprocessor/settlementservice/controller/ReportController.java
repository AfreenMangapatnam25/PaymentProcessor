package com.paymentprocessor.settlementservice.controller;

import com.paymentprocessor.settlementservice.service.ReportService;
import com.paymentprocessor.settlementservice.web.dto.report.DailySummaryReport;
import com.paymentprocessor.settlementservice.web.dto.report.ExceptionReport;
import com.paymentprocessor.settlementservice.web.dto.report.MerchantStatementReport;
import com.paymentprocessor.settlementservice.web.dto.report.PendingSettlementsReport;
import com.paymentprocessor.settlementservice.web.dto.report.ReserveReleaseScheduleReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Settlement reporting endpoints for merchants, finance, and operations. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/merchant-statement/{merchantId}")
    public MerchantStatementReport merchantStatement(@PathVariable String merchantId) {
        return reportService.merchantStatement(merchantId);
    }

    @GetMapping("/daily-summary")
    public DailySummaryReport dailySummary() {
        return reportService.dailySummary();
    }

    @GetMapping("/pending-settlements")
    public PendingSettlementsReport pendingSettlements() {
        return reportService.pendingSettlements();
    }

    @GetMapping("/reserve-release-schedule/{merchantId}")
    public ReserveReleaseScheduleReport reserveReleaseSchedule(@PathVariable String merchantId) {
        return reportService.reserveReleaseSchedule(merchantId);
    }

    @GetMapping("/exceptions")
    public ExceptionReport exceptions() {
        return reportService.exceptionReport();
    }
}
