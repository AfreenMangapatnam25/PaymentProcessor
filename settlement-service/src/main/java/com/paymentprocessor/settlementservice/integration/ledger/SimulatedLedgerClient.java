package com.paymentprocessor.settlementservice.integration.ledger;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process stand-in for the Ledger Service. Generates a journal id and logs
 * the posting. Replace with a real double-entry ledger integration in prod.
 */
@Component
public class SimulatedLedgerClient implements LedgerClient {

    private static final Logger log = LoggerFactory.getLogger(SimulatedLedgerClient.class);

    @Override
    public String postSettlement(String batchId, String merchantId, String currency,
                                 long netMinor, long feesMinor, long reserveMinor) {
        return journal("SETTLEMENT", "batch=" + batchId + " merchant=" + merchantId
                + " net=" + netMinor + " fees=" + feesMinor + " reserve=" + reserveMinor + " " + currency);
    }

    @Override
    public String postReserveHold(String reserveId, String merchantId, String currency, long amountMinor) {
        return journal("RESERVE_HOLD", "reserve=" + reserveId + " merchant=" + merchantId
                + " amount=" + amountMinor + " " + currency);
    }

    @Override
    public String postReserveRelease(String reserveId, String merchantId, String currency, long amountMinor) {
        return journal("RESERVE_RELEASE", "reserve=" + reserveId + " merchant=" + merchantId
                + " amount=" + amountMinor + " " + currency);
    }

    @Override
    public String postPayout(String payoutId, String merchantId, String currency, long amountMinor) {
        return journal("PAYOUT", "payout=" + payoutId + " merchant=" + merchantId
                + " amount=" + amountMinor + " " + currency);
    }

    @Override
    public String postPayoutReturn(String payoutId, String merchantId, String currency, long amountMinor) {
        return journal("PAYOUT_RETURN", "payout=" + payoutId + " merchant=" + merchantId
                + " amount=" + amountMinor + " " + currency);
    }

    @Override
    public String postReversal(String batchId, String merchantId, String currency, long amountMinor, String reason) {
        return journal("REVERSAL", "batch=" + batchId + " merchant=" + merchantId
                + " amount=" + amountMinor + " " + currency + " reason=" + reason);
    }

    @Override
    public String postAdjustment(String adjustmentId, String merchantId, String currency, long signedAmountMinor) {
        return journal("ADJUSTMENT", "adjustment=" + adjustmentId + " merchant=" + merchantId
                + " amount=" + signedAmountMinor + " " + currency);
    }

    private String journal(String type, String detail) {
        String journalId = "jrnl_" + UUID.randomUUID();
        log.info("Ledger posting [{}] {} -> {}", type, detail, journalId);
        return journalId;
    }
}
