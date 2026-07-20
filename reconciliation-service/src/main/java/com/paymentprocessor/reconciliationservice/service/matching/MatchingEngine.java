package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.MatchRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRun;
import com.paymentprocessor.reconciliationservice.domain.RecordMatchStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule-based reconciliation matching engine. Compares internal records against external records for
 * a run by applying the registered {@link MatchRule}s in priority order. The first rule that matches
 * a candidate pair produces a {@link MatchRecord}; matched external records are removed from the
 * candidate pool so each is used at most once.
 *
 * <p>Candidates for an internal record are narrowed by an amount index and a reference index so the
 * engine avoids a full cartesian scan while still covering both amount-based and reference-based rules.
 */
@Slf4j
@Component
public class MatchingEngine {

    private final List<MatchRule> rules;
    private final ReconProperties properties;

    public MatchingEngine(List<MatchRule> rules, ReconProperties properties) {
        // Defensive copy, ordered by rule priority (highest-confidence first).
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(MatchRule::priority))
                .toList();
        this.properties = properties;
    }

    public MatchingResult match(ReconRun run, List<ReconRecord> internals, List<ReconRecord> externals) {
        Map<String, List<ReconRecord>> byAmount = new HashMap<>();
        Map<String, List<ReconRecord>> byReference = new HashMap<>();
        for (ReconRecord ext : externals) {
            byAmount.computeIfAbsent(amountKey(ext.getAmount()), k -> new ArrayList<>()).add(ext);
            String ref = normalize(ext.getExternalReference());
            if (ref != null) {
                byReference.computeIfAbsent(ref, k -> new ArrayList<>()).add(ext);
            }
        }

        Set<ReconRecord> consumed = Collections.newSetFromMap(new IdentityHashMap<>());
        List<MatchRecord> matches = new ArrayList<>();
        List<ReconRecord> unmatchedInternal = new ArrayList<>();

        for (ReconRecord internal : internals) {
            MatchRecord match = findMatch(run, internal, byAmount, byReference, consumed);
            if (match != null) {
                matches.add(match);
                internal.setMatchStatus(RecordMatchStatus.MATCHED);
                match.getExternalRecord().setMatchStatus(RecordMatchStatus.MATCHED);
                consumed.add(match.getExternalRecord());
            } else {
                unmatchedInternal.add(internal);
            }
        }

        List<ReconRecord> unmatchedExternal = new ArrayList<>();
        for (ReconRecord ext : externals) {
            if (!consumed.contains(ext)) {
                unmatchedExternal.add(ext);
            }
        }

        log.debug("Run {}: matched={}, unmatchedInternal={}, unmatchedExternal={}",
                run.getUuid(), matches.size(), unmatchedInternal.size(), unmatchedExternal.size());
        return new MatchingResult(matches, unmatchedInternal, unmatchedExternal);
    }

    private MatchRecord findMatch(ReconRun run, ReconRecord internal,
                                  Map<String, List<ReconRecord>> byAmount,
                                  Map<String, List<ReconRecord>> byReference,
                                  Set<ReconRecord> consumed) {
        List<ReconRecord> candidates = candidatesFor(internal, byAmount, byReference);
        for (MatchRule rule : rules) {
            for (ReconRecord candidate : candidates) {
                if (consumed.contains(candidate)) {
                    continue;
                }
                MatchEvaluation eval = rule.evaluate(internal, candidate, properties.getMatching());
                if (eval != null) {
                    return toMatchRecord(run, internal, candidate, eval);
                }
            }
        }
        return null;
    }

    private List<ReconRecord> candidatesFor(ReconRecord internal,
                                            Map<String, List<ReconRecord>> byAmount,
                                            Map<String, List<ReconRecord>> byReference) {
        List<ReconRecord> candidates = new ArrayList<>();
        Set<ReconRecord> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ReconRecord> sameAmount = byAmount.get(amountKey(internal.getAmount()));
        if (sameAmount != null) {
            for (ReconRecord c : sameAmount) {
                if (seen.add(c)) {
                    candidates.add(c);
                }
            }
        }
        String ref = normalize(internal.getExternalReference());
        if (ref != null) {
            List<ReconRecord> sameRef = byReference.get(ref);
            if (sameRef != null) {
                for (ReconRecord c : sameRef) {
                    if (seen.add(c)) {
                        candidates.add(c);
                    }
                }
            }
        }
        return candidates;
    }

    private MatchRecord toMatchRecord(ReconRun run, ReconRecord internal, ReconRecord external, MatchEvaluation eval) {
        MatchRecord match = new MatchRecord();
        match.setReconRun(run);
        match.setInternalRecord(internal);
        match.setExternalRecord(external);
        match.setMatchType(eval.matchType());
        match.setMatchRule(eval.ruleName());
        match.setConfidence(eval.confidence());
        match.setAmountVariance(eval.amountVariance());
        match.setDateVarianceDays(eval.dateVarianceDays());
        match.setNote(eval.note());
        match.setManual(false);
        return match;
    }

    private String amountKey(BigDecimal amount) {
        return amount == null ? "null" : amount.stripTrailingZeros().toPlainString();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
