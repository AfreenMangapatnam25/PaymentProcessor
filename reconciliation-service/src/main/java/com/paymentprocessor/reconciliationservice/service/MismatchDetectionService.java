package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;
import com.paymentprocessor.reconciliationservice.domain.MismatchCategory;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRun;
import com.paymentprocessor.reconciliationservice.domain.RecordMatchStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classifies discrepancies remaining after matching into {@link ExceptionRecord}s. Handles duplicate
 * detection on each side, amount-mismatch pairing by shared reference, and the residual
 * missing-internal / missing-external cases.
 */
@Slf4j
@Service
public class MismatchDetectionService {

    private final SeverityScoringService severityScoringService;

    public MismatchDetectionService(SeverityScoringService severityScoringService) {
        this.severityScoringService = severityScoringService;
    }

    /** Result of duplicate detection: the deduplicated records and any duplicate exceptions raised. */
    public record DuplicateResult(List<ReconRecord> unique, List<ExceptionRecord> exceptions) {
    }

    /**
     * Detect records that repeat on the same side (same reference + amount). The first occurrence is
     * kept for matching; each subsequent occurrence becomes a DUPLICATE exception.
     */
    public DuplicateResult detectDuplicates(ReconRun run, List<ReconRecord> records) {
        Map<String, ReconRecord> firstSeen = new LinkedHashMap<>();
        List<ReconRecord> unique = new ArrayList<>();
        List<ExceptionRecord> exceptions = new ArrayList<>();

        for (ReconRecord record : records) {
            String key = duplicateKey(record);
            if (key != null && firstSeen.containsKey(key)) {
                record.setMatchStatus(RecordMatchStatus.EXCEPTION);
                exceptions.add(build(run, record, MismatchCategory.DUPLICATE,
                        "Duplicate " + record.getSource() + " record for reference "
                                + record.getExternalReference() + " amount " + record.getAmount(),
                        record.getAmount(), record.getAmount(), record.getAmount()));
            } else {
                if (key != null) {
                    firstSeen.put(key, record);
                }
                unique.add(record);
            }
        }
        return new DuplicateResult(unique, exceptions);
    }

    /**
     * Classify unmatched records. Internal and external records that share a reference (but failed to
     * match, therefore differ in amount beyond tolerance) become AMOUNT_MISMATCH; the remainder become
     * MISSING_EXTERNAL (unmatched internal) or MISSING_INTERNAL (unmatched external).
     */
    public List<ExceptionRecord> classifyUnmatched(ReconRun run,
                                                   List<ReconRecord> unmatchedInternal,
                                                   List<ReconRecord> unmatchedExternal) {
        List<ExceptionRecord> exceptions = new ArrayList<>();

        Map<String, ReconRecord> externalByRef = new LinkedHashMap<>();
        for (ReconRecord ext : unmatchedExternal) {
            String ref = normalize(ext.getExternalReference());
            if (ref != null) {
                externalByRef.putIfAbsent(ref, ext);
            }
        }

        Set<ReconRecord> pairedExternal = Collections.newSetFromMap(new IdentityHashMap<>());

        for (ReconRecord internal : unmatchedInternal) {
            String ref = normalize(internal.getExternalReference());
            ReconRecord ext = ref == null ? null : externalByRef.get(ref);
            if (ext != null && !pairedExternal.contains(ext)) {
                pairedExternal.add(ext);
                internal.setMatchStatus(RecordMatchStatus.EXCEPTION);
                ext.setMatchStatus(RecordMatchStatus.EXCEPTION);
                BigDecimal variance = internal.getAmount() == null || ext.getAmount() == null
                        ? null : internal.getAmount().subtract(ext.getAmount());
                exceptions.add(build(run, internal, MismatchCategory.AMOUNT_MISMATCH,
                        "Amount mismatch on reference " + internal.getExternalReference()
                                + ": internal " + internal.getAmount() + " vs external " + ext.getAmount()
                                + (variance == null ? "" : " (variance " + variance + ")"),
                        internal.getAmount(), internal.getAmount(), ext.getAmount()));
            } else {
                internal.setMatchStatus(RecordMatchStatus.EXCEPTION);
                exceptions.add(build(run, internal, MismatchCategory.MISSING_EXTERNAL,
                        "Internal record with no external counterpart (reference "
                                + internal.getExternalReference() + ")",
                        internal.getAmount(), internal.getAmount(), null));
            }
        }

        for (ReconRecord ext : unmatchedExternal) {
            if (pairedExternal.contains(ext)) {
                continue;
            }
            ext.setMatchStatus(RecordMatchStatus.EXCEPTION);
            exceptions.add(build(run, ext, MismatchCategory.MISSING_INTERNAL,
                    "External record with no internal counterpart (reference "
                            + ext.getExternalReference() + ")",
                    ext.getAmount(), null, ext.getAmount()));
        }

        return exceptions;
    }

    private ExceptionRecord build(ReconRun run, ReconRecord record, MismatchCategory category,
                                  String description, BigDecimal amount,
                                  BigDecimal expected, BigDecimal actual) {
        ExceptionRecord exception = new ExceptionRecord();
        exception.setReconRun(run);
        exception.setReconRecord(record);
        exception.setCategory(category);
        exception.setAmount(amount);
        exception.setCurrency(record.getCurrency());
        exception.setExpectedAmount(expected);
        exception.setActualAmount(actual);
        exception.setExternalReference(record.getExternalReference());
        exception.setDescription(description);
        exception.setAgeDays(0);
        severityScoringService.score(exception);
        return exception;
    }

    private String duplicateKey(ReconRecord record) {
        String ref = normalize(record.getExternalReference());
        if (ref == null || record.getAmount() == null) {
            return null;
        }
        return record.getSource() + "|" + ref + "|" + record.getAmount().stripTrailingZeros().toPlainString();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
