package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.domain.MatchRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;

import java.util.List;

/**
 * Output of a matching pass: the confirmed matches plus the internal and external records that
 * remain unmatched and must be handed to mismatch detection.
 */
public record MatchingResult(
        List<MatchRecord> matches,
        List<ReconRecord> unmatchedInternal,
        List<ReconRecord> unmatchedExternal
) {
}
