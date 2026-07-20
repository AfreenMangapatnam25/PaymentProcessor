package com.paymentprocessor.settlementservice.service.initiation;

import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.exception.BadRequestException;
import com.paymentprocessor.settlementservice.exception.RailException;
import com.paymentprocessor.settlementservice.integration.events.DomainEventPublisher;
import com.paymentprocessor.settlementservice.integration.events.EventTypes;
import com.paymentprocessor.settlementservice.integration.ledger.LedgerClient;
import com.paymentprocessor.settlementservice.integration.merchant.MerchantClient;
import com.paymentprocessor.settlementservice.integration.rail.RailAck;
import com.paymentprocessor.settlementservice.integration.rail.RailGateway;
import com.paymentprocessor.settlementservice.integration.rail.RailTransferRequest;
import com.paymentprocessor.settlementservice.service.PayoutService;
import com.paymentprocessor.settlementservice.service.SettlementBatchService;
import com.paymentprocessor.settlementservice.service.retry.RetryService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes the initiation flow: pre-validation, ledger posting, rail selection
 * (already decided during batching), transfer submission, state updates, and
 * event publication. Delegates failure handling to {@link RetryService}.
 */
@Service
public class InitiationService {

    private static final Logger log = LoggerFactory.getLogger(InitiationService.class);

    private final MerchantClient merchantClient;
    private final LedgerClient ledgerClient;
    private final RailGateway railGateway;
    private final DomainEventPublisher eventPublisher;
    private final PayoutService payoutService;
    private final SettlementBatchService batchService;
    private final RetryService retryService;

    public InitiationService(MerchantClient merchantClient,
                             LedgerClient ledgerClient,
                             RailGateway railGateway,
                             DomainEventPublisher eventPublisher,
                             PayoutService payoutService,
                             SettlementBatchService batchService,
                             RetryService retryService) {
        this.merchantClient = merchantClient;
        this.ledgerClient = ledgerClient;
        this.railGateway = railGateway;
        this.eventPublisher = eventPublisher;
        this.payoutService = payoutService;
        this.batchService = batchService;
        this.retryService = retryService;
    }

    /**
     * Validates, approves, posts to the ledger, and submits every payout in a
     * batch, then reconciles the batch status from the payout outcomes.
     */
    @Transactional
    public SettlementBatch initiateBatch(SettlementBatch batch) {
        if (batch.getStatus() != BatchStatus.PENDING) {
            throw new BadRequestException("Batch " + batch.getId() + " is not PENDING (is " + batch.getStatus() + ")");
        }
        preValidate(batch);

        // Approve (auto for standard merchants) and post the settlement to the ledger.
        batch.setApprovedBy("system");
        batch.setApprovedAt(Instant.now());
        batchService.transition(batch, BatchStatus.APPROVED);

        batch.setLedgerJournalId(ledgerClient.postSettlement(
                batch.getId(), batch.getMerchantId(), batch.getCurrency(),
                batch.getNetMinor(), batch.getFeesMinor() + batch.getSettlementFeeMinor(),
                batch.getReserveMinor()));
        batchService.transition(batch, BatchStatus.INITIATED);

        eventPublisher.publish(EventTypes.SETTLEMENT_INITIATED, "SettlementBatch", batch.getId(),
                Map.of("batchId", batch.getId(),
                        "merchantId", batch.getMerchantId(),
                        "netMinor", batch.getNetMinor(),
                        "currency", batch.getCurrency()));

        List<Payout> payouts = payoutService.findByBatch(batch.getId());
        for (Payout payout : payouts) {
            submitPayout(payout);
        }
        return reconcileBatchStatus(batch, payouts);
    }

    /** Re-submits a payout that was scheduled for retry. */
    @Transactional
    public void retryPayout(Payout payout) {
        SettlementBatch batch = batchService.findById(payout.getBatchId());
        // Re-open the batch for another initiation attempt if it had failed.
        if (batch.getStatus() == BatchStatus.FAILED) {
            batchService.transition(batch, BatchStatus.INITIATED);
        }
        submitPayout(payout);
        reconcileBatchStatus(batch, payoutService.findByBatch(batch.getId()));
    }

    /**
     * Simulated bank confirmation callback: moves a PROCESSING payout to
     * COMPLETED, posts the cash outflow, and completes the batch.
     */
    @Transactional
    public Payout confirmPayout(String payoutId) {
        Payout payout = payoutService.findById(payoutId);
        if (payout.getStatus() != PayoutStatus.PROCESSING && payout.getStatus() != PayoutStatus.INITIATED) {
            throw new BadRequestException("Payout " + payoutId + " is not awaiting confirmation");
        }
        completePayout(payout);
        SettlementBatch batch = batchService.findById(payout.getBatchId());
        reconcileBatchStatus(batch, payoutService.findByBatch(batch.getId()));
        return payout;
    }

    private void submitPayout(Payout payout) {
        if (payout.getStatus() != PayoutStatus.PENDING && payout.getStatus() != PayoutStatus.RETRY_SCHEDULED) {
            return;
        }
        payout.setAttemptCount(payout.getAttemptCount() + 1);
        payout.setSubmittedAt(Instant.now());
        payout.setNextRetryAt(null);
        payoutService.transition(payout, PayoutStatus.INITIATED);

        RailTransferRequest request = new RailTransferRequest(
                payout.getId(), payout.getRail(), payout.getMerchantId(),
                payout.getPayoutAccountId(), payout.getAmountMinor(),
                payout.getCurrency(), payout.getIdempotencyKey());
        try {
            RailAck ack = railGateway.submit(request);
            payout.setProviderRef(ack.providerRef());
            if (ack.processing()) {
                payoutService.transition(payout, PayoutStatus.PROCESSING);
                log.info("Payout {} submitted and processing (ref={})", payout.getId(), ack.providerRef());
            } else {
                completePayout(payout);
            }
        } catch (RailException ex) {
            retryService.handleFailure(payout, ex);
        }
    }

    private void completePayout(Payout payout) {
        payout.setPaidAt(Instant.now());
        payout.setLedgerJournalId(ledgerClient.postPayout(
                payout.getId(), payout.getMerchantId(), payout.getCurrency(), payout.getAmountMinor()));
        payoutService.transition(payout, PayoutStatus.COMPLETED);
        eventPublisher.publish(EventTypes.SETTLEMENT_COMPLETED, "Payout", payout.getId(),
                Map.of("payoutId", payout.getId(),
                        "merchantId", payout.getMerchantId(),
                        "batchId", payout.getBatchId(),
                        "amountMinor", payout.getAmountMinor(),
                        "currency", payout.getCurrency()));
        log.info("Payout {} completed", payout.getId());
    }

    private void preValidate(SettlementBatch batch) {
        var profile = merchantClient.getProfile(batch.getMerchantId())
                .orElseThrow(() -> new BadRequestException("Unknown merchant " + batch.getMerchantId()));
        if (!profile.active()) {
            throw new BadRequestException("Merchant " + batch.getMerchantId() + " is not active");
        }
        if (!merchantClient.isPayoutAccountValid(batch.getMerchantId(), profile.payoutAccountId())) {
            throw new BadRequestException("Merchant " + batch.getMerchantId() + " has no valid payout account");
        }
    }

    private SettlementBatch reconcileBatchStatus(SettlementBatch batch, List<Payout> payouts) {
        boolean allCompleted = payouts.stream().allMatch(p -> p.getStatus() == PayoutStatus.COMPLETED);
        boolean anyActive = payouts.stream().anyMatch(p ->
                p.getStatus() == PayoutStatus.PROCESSING
                        || p.getStatus() == PayoutStatus.INITIATED
                        || p.getStatus() == PayoutStatus.RETRY_SCHEDULED);
        boolean anyFailed = payouts.stream().anyMatch(p -> p.getStatus() == PayoutStatus.FAILED);

        if (allCompleted) {
            batch.setClosedAt(Instant.now());
            return batchService.transition(batch, BatchStatus.COMPLETED);
        }
        if (anyActive) {
            if (batch.getStatus() == BatchStatus.INITIATED) {
                return batchService.transition(batch, BatchStatus.PROCESSING);
            }
            return batch;
        }
        if (anyFailed) {
            batch.setFailureReason("One or more payouts failed");
            return batchService.transition(batch, BatchStatus.FAILED);
        }
        return batch;
    }
}
