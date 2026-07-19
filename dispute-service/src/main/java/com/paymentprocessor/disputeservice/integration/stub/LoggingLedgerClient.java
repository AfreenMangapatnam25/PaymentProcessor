package com.paymentprocessor.disputeservice.integration.stub;

import com.paymentprocessor.disputeservice.integration.LedgerClient;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development stub for {@link LedgerClient}. Logs the intended postings and
 * returns synthetic journal ids. Replace with a real Ledger Service client
 * (REST / messaging) in production.
 */
@Component
public class LoggingLedgerClient implements LedgerClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingLedgerClient.class);

    @Override
    public String postChargebackDebit(String disputeId, String merchantId,
                                      long amountMinor, long feeMinor, String currency) {
        String journalId = "jrn_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[LEDGER] Chargeback debit dispute={} merchant={} amount={} fee={} {} -> journal={}",
                disputeId, merchantId, amountMinor, feeMinor, currency, journalId);
        return journalId;
    }

    @Override
    public String reverseChargeback(String disputeId, String originalJournalId) {
        String journalId = "jrn_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[LEDGER] Reverse chargeback dispute={} original={} -> reversal={}",
                disputeId, originalJournalId, journalId);
        return journalId;
    }

    @Override
    public String finalizeLoss(String disputeId, String originalJournalId) {
        String journalId = "jrn_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[LEDGER] Finalize loss dispute={} original={} -> settlement={}",
                disputeId, originalJournalId, journalId);
        return journalId;
    }

    @Override
    public String postFee(String disputeId, long feeMinor, String currency, String description) {
        String journalId = "jrn_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[LEDGER] Fee posting dispute={} fee={} {} desc='{}' -> journal={}",
                disputeId, feeMinor, currency, description, journalId);
        return journalId;
    }
}
