package com.paymentprocessor.settlementservice.service.reconciliation;

import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.exception.BadRequestException;
import com.paymentprocessor.settlementservice.service.SettlementBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Matches internal settlement records against external bank statements. In this
 * reference implementation the match is simulated; a production build would
 * parse MT940 / CAMT.053 files and compare per-transfer references and amounts.
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final SettlementBatchService batchService;

    public ReconciliationService(SettlementBatchService batchService) {
        this.batchService = batchService;
    }

    /**
     * Marks a completed batch as reconciled once its records match the external
     * bank statement.
     */
    @Transactional
    public SettlementBatch reconcile(String batchId) {
        SettlementBatch batch = batchService.findById(batchId);
        if (batch.getStatus() != BatchStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED batches can be reconciled (batch "
                    + batchId + " is " + batch.getStatus() + ")");
        }
        SettlementBatch reconciled = batchService.transition(batch, BatchStatus.RECONCILED);
        log.info("Reconciled batch {}", batchId);
        return reconciled;
    }
}
