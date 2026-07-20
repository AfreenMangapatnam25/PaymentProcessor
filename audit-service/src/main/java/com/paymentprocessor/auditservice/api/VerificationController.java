package com.paymentprocessor.auditservice.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paymentprocessor.auditservice.api.dto.ChainVerificationResponse;
import com.paymentprocessor.auditservice.service.AuditQueryService;
import com.paymentprocessor.auditservice.service.ChainVerificationService;
import com.paymentprocessor.auditservice.service.ChainVerificationService.VerificationResult;

/**
 * Integrity verification endpoints. Recomputes the hash chain to prove the trail has
 * not been tampered with. Intended for auditors, compliance jobs and monitoring.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class VerificationController {

    private final ChainVerificationService verification;
    private final AuditQueryService query;

    public VerificationController(ChainVerificationService verification, AuditQueryService query) {
        this.verification = verification;
        this.query = query;
    }

    /**
     * Verifies the chain. With no parameters, verifies the whole chain from seq 1 to the
     * current head. A range can be supplied for incremental/partial checks.
     */
    @GetMapping("/verify")
    public ChainVerificationResponse verify(
            @RequestParam(required = false) Long fromSeq,
            @RequestParam(required = false) Long toSeq) {
        long from = fromSeq != null ? fromSeq : 1L;
        long to = toSeq != null ? toSeq : query.headSeq();
        VerificationResult result = verification.verifyRange(from, to);
        return ChainVerificationResponse.from(result);
    }

    /** Current head sequence of the chain (0 when empty). */
    @GetMapping("/head")
    public HeadResponse head() {
        return new HeadResponse(query.headSeq());
    }

    public record HeadResponse(long headSeq) {
    }
}
