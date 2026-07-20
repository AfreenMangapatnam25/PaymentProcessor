package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.enums.ScheduleType;
import com.paymentprocessor.settlementservice.repository.SettlementItemRepository;
import com.paymentprocessor.settlementservice.service.batch.BatchingService;
import com.paymentprocessor.settlementservice.service.initiation.InitiationService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a full settlement cycle: for every merchant/currency with pending
 * items it builds a batch and, if one is produced, initiates the payout. Each
 * merchant is processed in its own transaction so one failure does not abort the
 * whole run.
 */
@Service
public class SettlementRunService {

    private static final Logger log = LoggerFactory.getLogger(SettlementRunService.class);

    private final SettlementItemRepository itemRepository;
    private final BatchingService batchingService;
    private final InitiationService initiationService;

    public SettlementRunService(SettlementItemRepository itemRepository,
                                BatchingService batchingService,
                                InitiationService initiationService) {
        this.itemRepository = itemRepository;
        this.batchingService = batchingService;
        this.initiationService = initiationService;
    }

    /** Runs the settlement cycle across all merchants with pending items. */
    public RunSummary runCycle(ScheduleType scheduleType) {
        var pending = itemRepository.findPendingMerchantCurrencies();
        int created = 0;
        int initiated = 0;
        for (var mc : pending) {
            try {
                Optional<SettlementBatch> batch = runForMerchant(mc.getMerchantId(), mc.getCurrency(), scheduleType);
                if (batch.isPresent()) {
                    created++;
                    initiated++;
                }
            } catch (RuntimeException ex) {
                log.error("Settlement failed for merchant {} {}: {}",
                        mc.getMerchantId(), mc.getCurrency(), ex.getMessage(), ex);
            }
        }
        log.info("Settlement cycle [{}] complete: {} batches created, {} initiated",
                scheduleType, created, initiated);
        return new RunSummary(created, initiated);
    }

    /**
     * Builds and initiates a settlement for a single merchant + currency.
     * Batch creation and initiation run in their own transactions (in the
     * respective services) so a persisted batch survives an initiation error and
     * can be retried.
     */
    public Optional<SettlementBatch> runForMerchant(String merchantId, String currency, ScheduleType scheduleType) {
        Optional<SettlementBatch> batch = batchingService.createBatch(merchantId, currency, scheduleType);
        batch.ifPresent(initiationService::initiateBatch);
        return batch;
    }

    /** Summary of a settlement cycle run. */
    public record RunSummary(int batchesCreated, int batchesInitiated) {
    }
}
