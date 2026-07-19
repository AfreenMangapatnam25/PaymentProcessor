package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.entity.Reserve;
import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.enums.ReserveStatus;
import com.paymentprocessor.settlementservice.repository.PayoutRepository;
import com.paymentprocessor.settlementservice.repository.ReserveRepository;
import com.paymentprocessor.settlementservice.repository.SettlementBatchRepository;
import com.paymentprocessor.settlementservice.web.dto.report.DailySummaryReport;
import com.paymentprocessor.settlementservice.web.dto.report.ExceptionReport;
import com.paymentprocessor.settlementservice.web.dto.report.MerchantStatementReport;
import com.paymentprocessor.settlementservice.web.dto.report.PendingSettlementsReport;
import com.paymentprocessor.settlementservice.web.dto.report.ReserveReleaseScheduleReport;
import com.paymentprocessor.settlementservice.web.mapper.SettlementMapper;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces the settlement reports described in the README: merchant statements,
 * daily summaries, pending settlements, reserve-release schedules, and exception
 * reports. All methods are read-only.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final SettlementBatchRepository batchRepository;
    private final PayoutRepository payoutRepository;
    private final ReserveRepository reserveRepository;
    private final SettlementMapper mapper;

    public ReportService(SettlementBatchRepository batchRepository,
                         PayoutRepository payoutRepository,
                         ReserveRepository reserveRepository,
                         SettlementMapper mapper) {
        this.batchRepository = batchRepository;
        this.payoutRepository = payoutRepository;
        this.reserveRepository = reserveRepository;
        this.mapper = mapper;
    }

    public MerchantStatementReport merchantStatement(String merchantId) {
        List<SettlementBatch> batches = batchRepository.findByMerchantId(merchantId);
        List<Payout> payouts = payoutRepository.findByMerchantId(merchantId);
        List<Reserve> reserves = reserveRepository.findByMerchantId(merchantId);
        long totalSettled = batches.stream()
                .filter(b -> b.getStatus() == BatchStatus.COMPLETED || b.getStatus() == BatchStatus.RECONCILED)
                .mapToLong(SettlementBatch::getNetMinor)
                .sum();
        return new MerchantStatementReport(merchantId, Instant.now(), totalSettled,
                mapper.toBatchResponses(batches),
                mapper.toPayoutResponses(payouts),
                mapper.toReserveResponses(reserves));
    }

    public DailySummaryReport dailySummary() {
        Map<String, List<SettlementBatch>> grouped = batchRepository.findAll().stream()
                .collect(Collectors.groupingBy(b -> b.getCurrency() + "|" + b.getStatus().name()));
        List<DailySummaryReport.Line> lines = grouped.values().stream()
                .map(group -> {
                    SettlementBatch first = group.get(0);
                    long total = group.stream().mapToLong(SettlementBatch::getNetMinor).sum();
                    return new DailySummaryReport.Line(first.getCurrency(), first.getStatus(),
                            group.size(), total);
                })
                .sorted(Comparator.comparing(DailySummaryReport.Line::currency)
                        .thenComparing(l -> l.status().name()))
                .toList();
        return new DailySummaryReport(Instant.now(), lines);
    }

    public PendingSettlementsReport pendingSettlements() {
        List<SettlementBatch> batches = batchRepository.findByStatusIn(List.of(
                BatchStatus.PENDING, BatchStatus.APPROVED, BatchStatus.INITIATED, BatchStatus.PROCESSING));
        List<Payout> payouts = payoutRepository.findByStatusIn(List.of(
                PayoutStatus.PENDING, PayoutStatus.INITIATED, PayoutStatus.PROCESSING,
                PayoutStatus.RETRY_SCHEDULED));
        return new PendingSettlementsReport(Instant.now(),
                mapper.toBatchResponses(batches), mapper.toPayoutResponses(payouts));
    }

    public ReserveReleaseScheduleReport reserveReleaseSchedule(String merchantId) {
        List<Reserve> held = reserveRepository.findByMerchantIdAndStatus(merchantId, ReserveStatus.HELD).stream()
                .sorted(Comparator.comparing(Reserve::getHoldUntil,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        long totalHeld = held.stream().mapToLong(Reserve::remainingMinor).sum();
        return new ReserveReleaseScheduleReport(Instant.now(), totalHeld, mapper.toReserveResponses(held));
    }

    public ExceptionReport exceptionReport() {
        List<SettlementBatch> batches = batchRepository.findByStatusIn(List.of(
                BatchStatus.FAILED, BatchStatus.REVERSED));
        List<Payout> payouts = payoutRepository.findByStatusIn(List.of(
                PayoutStatus.FAILED, PayoutStatus.RETURNED, PayoutStatus.REVERSED));
        return new ExceptionReport(Instant.now(),
                mapper.toBatchResponses(batches), mapper.toPayoutResponses(payouts));
    }
}
