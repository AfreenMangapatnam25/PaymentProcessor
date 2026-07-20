package com.paymentprocessor.settlementservice.service.reversal;

import com.paymentprocessor.settlementservice.config.SettlementProperties;
import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.enums.ApprovalLevel;
import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.exception.ApprovalRequiredException;
import com.paymentprocessor.settlementservice.exception.BadRequestException;
import com.paymentprocessor.settlementservice.integration.events.DomainEventPublisher;
import com.paymentprocessor.settlementservice.integration.events.EventTypes;
import com.paymentprocessor.settlementservice.integration.ledger.LedgerClient;
import com.paymentprocessor.settlementservice.service.PayoutService;
import com.paymentprocessor.settlementservice.service.SettlementBatchService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recovers funds from a merchant when a settlement was issued in error or later
 * invalidated (erroneous payout, subsequent chargeback, fraud, regulatory order,
 * merchant termination). Posts a compensating ledger entry and drives the batch
 * and its payouts to REVERSED.
 */
@Service
public class ReversalService {

    private static final Logger log = LoggerFactory.getLogger(ReversalService.class);

    private final SettlementBatchService batchService;
    private final PayoutService payoutService;
    private final LedgerClient ledgerClient;
    private final DomainEventPublisher eventPublisher;
    private final SettlementProperties properties;

    public ReversalService(SettlementBatchService batchService,
                           PayoutService payoutService,
                           LedgerClient ledgerClient,
                           DomainEventPublisher eventPublisher,
                           SettlementProperties properties) {
        this.batchService = batchService;
        this.payoutService = payoutService;
        this.ledgerClient = ledgerClient;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    /**
     * Reverses a settled batch.
     *
     * @param reason        business reason for the reversal
     * @param approver      the operator performing the reversal
     * @param approverLevel the approver's authority level
     */
    @Transactional
    public SettlementBatch reverse(String batchId, String reason, String approver, ApprovalLevel approverLevel) {
        SettlementBatch batch = batchService.findById(batchId);
        if (batch.getStatus() != BatchStatus.COMPLETED && batch.getStatus() != BatchStatus.RECONCILED) {
            throw new BadRequestException("Only COMPLETED or RECONCILED batches can be reversed (batch "
                    + batchId + " is " + batch.getStatus() + ")");
        }
        requireApproval(batch.getNetMinor(), approverLevel);

        ledgerClient.postReversal(batch.getId(), batch.getMerchantId(), batch.getCurrency(),
                batch.getNetMinor(), reason);

        List<Payout> payouts = payoutService.findByBatch(batchId);
        for (Payout payout : payouts) {
            if (payout.getStatus() == PayoutStatus.COMPLETED || payout.getStatus() == PayoutStatus.RETURNED) {
                payout.setFailureReason("Reversed: " + reason);
                payoutService.transition(payout, PayoutStatus.REVERSED);
            }
        }

        batch.setFailureReason("Reversed by " + approver + ": " + reason);
        SettlementBatch reversed = batchService.transition(batch, BatchStatus.REVERSED);

        eventPublisher.publish(EventTypes.SETTLEMENT_REVERSED, "SettlementBatch", batchId,
                Map.of("batchId", batchId,
                        "merchantId", batch.getMerchantId(),
                        "amountMinor", batch.getNetMinor(),
                        "currency", batch.getCurrency(),
                        "reason", reason,
                        "approvedBy", approver));
        log.info("Reversed batch {} amount {} {} reason='{}' by {}",
                batchId, batch.getNetMinor(), batch.getCurrency(), reason, approver);
        return reversed;
    }

    private void requireApproval(long amountMinor, ApprovalLevel approverLevel) {
        long threshold = properties.getApproval().getReversalFinanceDirectorThresholdMinor();
        if (amountMinor > threshold && approverLevel != ApprovalLevel.FINANCE_DIRECTOR) {
            throw new ApprovalRequiredException("Reversal of " + amountMinor
                    + " requires FINANCE_DIRECTOR approval but got " + approverLevel);
        }
    }
}
