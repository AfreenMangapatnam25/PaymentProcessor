package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.entity.PayoutReturn;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.integration.events.DomainEventPublisher;
import com.paymentprocessor.settlementservice.integration.events.EventTypes;
import com.paymentprocessor.settlementservice.integration.ledger.LedgerClient;
import com.paymentprocessor.settlementservice.repository.PayoutReturnRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes funds returned by a receiving bank after a payout was submitted
 * (invalid/closed account, etc.): records the return, posts a compensating
 * ledger entry, moves the payout to RETURNED, and publishes an event.
 */
@Service
public class PayoutReturnService {

    private static final Logger log = LoggerFactory.getLogger(PayoutReturnService.class);

    private final PayoutReturnRepository repository;
    private final PayoutService payoutService;
    private final LedgerClient ledgerClient;
    private final DomainEventPublisher eventPublisher;

    public PayoutReturnService(PayoutReturnRepository repository,
                               PayoutService payoutService,
                               LedgerClient ledgerClient,
                               DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.payoutService = payoutService;
        this.ledgerClient = ledgerClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Records a bank return against a payout and drives the payout to RETURNED.
     */
    @Transactional
    public PayoutReturn recordReturn(String payoutId, String reasonCode, String reasonDescription) {
        Payout payout = payoutService.findById(payoutId);

        PayoutReturn ret = new PayoutReturn();
        ret.setId("ret_" + UUID.randomUUID());
        ret.setPayoutId(payoutId);
        ret.setReasonCode(reasonCode);
        ret.setReasonDescription(reasonDescription);
        ret.setAmountMinor(payout.getAmountMinor());
        ret.setCurrency(payout.getCurrency());
        ret.setReturnedAt(Instant.now());
        ret.setLedgerJournalId(ledgerClient.postPayoutReturn(
                payoutId, payout.getMerchantId(), payout.getCurrency(), payout.getAmountMinor()));
        PayoutReturn saved = repository.save(ret);

        payout.setFailureCode(reasonCode);
        payout.setFailureReason(reasonDescription);
        payoutService.transition(payout, PayoutStatus.RETURNED);

        eventPublisher.publish(EventTypes.PAYOUT_RETURNED, "Payout", payoutId,
                Map.of("payoutId", payoutId,
                        "merchantId", payout.getMerchantId(),
                        "amountMinor", payout.getAmountMinor(),
                        "currency", payout.getCurrency(),
                        "reasonCode", reasonCode));
        log.info("Recorded return {} for payout {} reason {}", saved.getId(), payoutId, reasonCode);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PayoutReturn> findByPayout(String payoutId) {
        return repository.findByPayoutId(payoutId);
    }

    @Transactional(readOnly = true)
    public List<PayoutReturn> findAll() {
        return repository.findAll();
    }
}
