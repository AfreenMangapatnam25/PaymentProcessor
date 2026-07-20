package com.paymentprocessor.disputeservice.service;

import com.paymentprocessor.disputeservice.config.NetworkRules;
import com.paymentprocessor.disputeservice.config.NetworkRules.ReserveTier;
import com.paymentprocessor.disputeservice.domain.enums.LiabilityParty;
import com.paymentprocessor.disputeservice.entity.Dispute;
import com.paymentprocessor.disputeservice.entity.Liability;
import com.paymentprocessor.disputeservice.integration.LedgerClient;
import com.paymentprocessor.disputeservice.integration.SettlementClient;
import com.paymentprocessor.disputeservice.repository.LiabilityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the financial impact of a dispute: recording the chargeback debit and
 * fee on receipt, applying the reserve tier, reversing funds on a win, and
 * finalising the loss when a dispute is lost or accepted. All monetary movement
 * is delegated to the Ledger and Settlement services.
 */
@Service
public class LiabilityService {

    private final LiabilityRepository repository;
    private final LedgerClient ledgerClient;
    private final SettlementClient settlementClient;
    private final NetworkRules networkRules;

    public LiabilityService(LiabilityRepository repository, LedgerClient ledgerClient,
                            SettlementClient settlementClient, NetworkRules networkRules) {
        this.repository = repository;
        this.ledgerClient = ledgerClient;
        this.settlementClient = settlementClient;
        this.networkRules = networkRules;
    }

    /**
     * Records the financial impact when a chargeback is received: posts the debit
     * to the ledger, recovers funds from settlement, and applies the reserve tier
     * implied by the merchant's chargeback rate.
     *
     * @param dispute              the newly created dispute
     * @param chargebackRatePercent merchant's current chargeback rate, as a percentage
     * @return the persisted liability record
     */
    @Transactional
    public Liability recordChargebackImpact(Dispute dispute, double chargebackRatePercent) {
        long amount = dispute.getAmountMinor() == null ? 0L : dispute.getAmountMinor();
        long fee = dispute.getChargebackFeeMinor() == null
                ? networkRules.chargebackFeeMinor(dispute.getNetwork())
                : dispute.getChargebackFeeMinor();

        String journalId = ledgerClient.postChargebackDebit(
                dispute.getId(), dispute.getMerchantId(), amount, fee, dispute.getCurrency());
        settlementClient.recoverFromSettlement(
                dispute.getMerchantId(), dispute.getId(), amount + fee, dispute.getCurrency());

        ReserveTier tier = networkRules.reserveTier(chargebackRatePercent);
        settlementClient.adjustReserve(dispute.getMerchantId(), tier.percentage(), tier.rollingDays());

        Liability liability = new Liability();
        liability.setDisputeId(dispute.getId());
        liability.setParty(LiabilityParty.PENDING);
        liability.setDisputedAmountMinor(amount);
        liability.setFeeMinor(fee);
        liability.setTotalMinor(amount + fee);
        liability.setCurrency(dispute.getCurrency());
        liability.setReserveTier(tier.label());
        liability.setReservePercentage(tier.percentage());
        liability.setLedgerJournalId(journalId);
        return repository.save(liability);
    }

    /**
     * Reverses the chargeback when a dispute is won, returning funds to the
     * merchant.
     *
     * @return the ledger reversal journal id
     */
    @Transactional
    public String reverseOnWin(Dispute dispute) {
        Liability liability = current(dispute.getId()).orElse(null);
        String originalJournal = liability != null ? liability.getLedgerJournalId() : null;

        String reversalJournal = ledgerClient.reverseChargeback(dispute.getId(), originalJournal);
        settlementClient.releaseToMerchant(dispute.getMerchantId(), dispute.getId(),
                dispute.totalExposureMinor(), dispute.getCurrency());

        if (liability != null) {
            liability.setParty(LiabilityParty.PLATFORM);
            liability.setReversed(true);
            liability.setReversedAt(Instant.now());
            repository.save(liability);
        }
        return reversalJournal;
    }

    /**
     * Finalises the loss when a dispute is lost or liability is accepted, moving
     * the held funds from reserve to platform revenue.
     */
    @Transactional
    public void finalizeLoss(Dispute dispute) {
        Liability liability = current(dispute.getId()).orElse(null);
        String originalJournal = liability != null ? liability.getLedgerJournalId() : null;
        ledgerClient.finalizeLoss(dispute.getId(), originalJournal);
        if (liability != null) {
            liability.setParty(LiabilityParty.MERCHANT);
            repository.save(liability);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Liability> current(String disputeId) {
        return repository.findFirstByDisputeIdOrderByRecordedAtDesc(disputeId);
    }

    @Transactional(readOnly = true)
    public List<Liability> history(String disputeId) {
        return repository.findByDisputeId(disputeId);
    }
}
