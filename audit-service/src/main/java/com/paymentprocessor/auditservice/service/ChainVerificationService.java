package com.paymentprocessor.auditservice.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paymentprocessor.auditservice.config.AuditProperties;
import com.paymentprocessor.auditservice.domain.AuditRecord;
import com.paymentprocessor.auditservice.repository.AuditRecordRepository;

/**
 * Re-verifies the tamper-evident chain by recomputing every hash and checking that each
 * record links to its predecessor. Any recomputed hash that differs from the stored one,
 * a broken {@code prevHash} link, or a gap in {@code seq}, indicates tampering.
 */
@Service
public class ChainVerificationService {

    private static final Logger log = LoggerFactory.getLogger(ChainVerificationService.class);
    private static final int PAGE = 1000;

    private final AuditRecordRepository records;
    private final HashChainService hashChain;
    private final String genesisHash;

    public ChainVerificationService(AuditRecordRepository records,
                                    HashChainService hashChain,
                                    AuditProperties props) {
        this.records = records;
        this.hashChain = hashChain;
        this.genesisHash = props.getChain().getGenesisHash();
    }

    /** Result of a verification run over a sequence range. */
    public record VerificationResult(
            boolean valid,
            long fromSeq,
            long toSeq,
            long recordsChecked,
            Long firstBrokenSeq,
            String detail) {
    }

    /**
     * Verifies the closed sequence range {@code [fromSeq, toSeq]}. The record preceding
     * {@code fromSeq} (or genesis, if {@code fromSeq <= 1}) supplies the expected first
     * {@code prevHash}, so partial ranges are still fully validated.
     */
    public VerificationResult verifyRange(long fromSeq, long toSeq) {
        if (fromSeq < 1 || toSeq < fromSeq) {
            return new VerificationResult(false, fromSeq, toSeq, 0, null,
                    "Invalid range: fromSeq must be >= 1 and <= toSeq");
        }

        String expectedPrevHash = expectedPrevHashBefore(fromSeq);
        long checked = 0;
        long expectedSeq = fromSeq;

        for (long start = fromSeq; start <= toSeq; start += PAGE) {
            long end = Math.min(start + PAGE - 1, toSeq);
            List<AuditRecord> batch =
                    records.findBySeqGreaterThanEqualAndSeqLessThanEqualOrderBySeqAsc(start, end);

            for (AuditRecord r : batch) {
                if (r.getSeq() != expectedSeq) {
                    return broken(fromSeq, toSeq, checked, expectedSeq,
                            "Sequence gap: expected seq=" + expectedSeq + " but found seq=" + r.getSeq());
                }
                if (!expectedPrevHash.equals(r.getPrevHash())) {
                    return broken(fromSeq, toSeq, checked, r.getSeq(),
                            "Broken link at seq=" + r.getSeq() + ": prev_hash does not match "
                                    + "the previous record's hash");
                }
                String recomputed = hashChain.computeHash(r);
                if (!recomputed.equals(r.getHash())) {
                    return broken(fromSeq, toSeq, checked, r.getSeq(),
                            "Hash mismatch at seq=" + r.getSeq() + ": record content has been altered");
                }
                expectedPrevHash = r.getHash();
                expectedSeq++;
                checked++;
            }
        }

        if (checked == 0) {
            return new VerificationResult(true, fromSeq, toSeq, 0, null, "No records in range");
        }
        log.info("Chain verification OK for seq [{}, {}] ({} records).", fromSeq, toSeq, checked);
        return new VerificationResult(true, fromSeq, toSeq, checked, null, "Chain intact");
    }

    private String expectedPrevHashBefore(long fromSeq) {
        if (fromSeq <= 1) {
            return genesisHash;
        }
        return records.findBySeq(fromSeq - 1)
                .map(AuditRecord::getHash)
                .orElse(genesisHash);
    }

    private VerificationResult broken(long fromSeq, long toSeq, long checked,
                                      long brokenSeq, String detail) {
        log.error("Chain verification FAILED at seq={}: {}", brokenSeq, detail);
        return new VerificationResult(false, fromSeq, toSeq, checked, brokenSeq, detail);
    }
}
