package com.paymentprocessor.disputeservice.dto.response;

import com.paymentprocessor.disputeservice.domain.enums.LiabilityParty;
import com.paymentprocessor.disputeservice.entity.Liability;
import java.time.Instant;

/**
 * API view of a dispute's financial impact record.
 */
public record LiabilityResponse(
        String id,
        String disputeId,
        LiabilityParty party,
        Long disputedAmountMinor,
        Long feeMinor,
        Long totalMinor,
        String currency,
        String reserveTier,
        Integer reservePercentage,
        String ledgerJournalId,
        boolean reversed,
        Instant recordedAt,
        Instant reversedAt) {

    public static LiabilityResponse from(Liability l) {
        return new LiabilityResponse(l.getId(), l.getDisputeId(), l.getParty(),
                l.getDisputedAmountMinor(), l.getFeeMinor(), l.getTotalMinor(), l.getCurrency(),
                l.getReserveTier(), l.getReservePercentage(), l.getLedgerJournalId(),
                l.isReversed(), l.getRecordedAt(), l.getReversedAt());
    }
}
