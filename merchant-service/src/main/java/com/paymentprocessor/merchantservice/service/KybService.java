package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.enums.KybCaseStatus;
import com.paymentprocessor.merchantservice.common.enums.KybStatus;
import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.KybCaseResponse;
import com.paymentprocessor.merchantservice.dto.KybDecisionRequest;
import com.paymentprocessor.merchantservice.dto.KybDocumentRequest;
import com.paymentprocessor.merchantservice.dto.KybDocumentResponse;
import com.paymentprocessor.merchantservice.entity.KybCase;
import com.paymentprocessor.merchantservice.entity.KybDocument;
import com.paymentprocessor.merchantservice.repository.KybCaseRepository;
import com.paymentprocessor.merchantservice.repository.KybDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tracks KYB cases and their supporting documents. Verification logic itself is owned by the
 * external Compliance/KYB Service; this service records submissions and applies decisions,
 * propagating the resulting KYB status onto the merchant.
 */
@Service
public class KybService {

    private final KybCaseRepository caseRepository;
    private final KybDocumentRepository documentRepository;
    private final MerchantService merchantService;

    public KybService(KybCaseRepository caseRepository, KybDocumentRepository documentRepository,
                      MerchantService merchantService) {
        this.caseRepository = caseRepository;
        this.documentRepository = documentRepository;
        this.merchantService = merchantService;
    }

    @Transactional(readOnly = true)
    public List<KybCaseResponse> listCases(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return caseRepository.findByMerchantId(merchantId).stream().map(this::toCaseResponse).toList();
    }

    @Transactional(readOnly = true)
    public KybCaseResponse latestCase(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return caseRepository.findFirstByMerchantIdOrderByCreatedAtDesc(merchantId)
                .map(this::toCaseResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("KybCase for merchant", merchantId));
    }

    @Transactional
    public KybCaseResponse submitCase(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        KybCase c = new KybCase();
        c.setMerchantId(merchantId);
        c.setStatus(KybCaseStatus.IN_REVIEW);
        c.setSubmittedAt(Instant.now());
        return toCaseResponse(caseRepository.save(c));
    }

    /** Applies a decision received from the Compliance/KYB Service and updates merchant KYB status. */
    @Transactional
    public KybCaseResponse applyDecision(UUID merchantId, UUID caseId, KybDecisionRequest req) {
        merchantService.assertAccessible(merchantId);
        KybCase c = caseRepository.findByIdAndMerchantId(caseId, merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("KybCase", caseId));
        c.setStatus(req.decision());
        c.setRiskScore(req.riskScore());
        c.setSanctionsScreened(req.sanctionsScreened());
        c.setPepScreened(req.pepScreened());
        c.setDecisionReason(req.decisionReason());
        c.setExternalReference(req.externalReference());
        c.setReviewedAt(Instant.now());

        merchantService.applyKybStatus(merchantId, mapToKybStatus(req.decision()));
        return toCaseResponse(c);
    }

    @Transactional(readOnly = true)
    public List<KybDocumentResponse> listDocuments(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return documentRepository.findByMerchantId(merchantId).stream().map(this::toDocResponse).toList();
    }

    @Transactional
    public KybDocumentResponse addDocument(UUID merchantId, KybDocumentRequest req) {
        merchantService.assertAccessible(merchantId);
        KybDocument d = new KybDocument();
        d.setMerchantId(merchantId);
        caseRepository.findFirstByMerchantIdOrderByCreatedAtDesc(merchantId)
                .ifPresent(c -> d.setKybCaseId(c.getId()));
        d.setDocumentType(req.documentType());
        d.setFileName(req.fileName());
        d.setContentType(req.contentType());
        d.setStorageReference(req.storageReference());
        d.setExpiresOn(req.expiresOn());
        return toDocResponse(documentRepository.save(d));
    }

    @Transactional
    public void deleteDocument(UUID merchantId, UUID documentId) {
        merchantService.assertAccessible(merchantId);
        KybDocument d = documentRepository.findByIdAndMerchantId(documentId, merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("KybDocument", documentId));
        documentRepository.delete(d);
    }

    private KybStatus mapToKybStatus(KybCaseStatus decision) {
        return switch (decision) {
            case APPROVED -> KybStatus.VERIFIED;
            case REJECTED -> KybStatus.FAILED;
            case OPEN, IN_REVIEW -> KybStatus.PENDING;
        };
    }

    private KybCaseResponse toCaseResponse(KybCase c) {
        return new KybCaseResponse(c.getId(), c.getMerchantId(), c.getStatus(), c.getExternalReference(),
                c.getRiskScore(), c.isSanctionsScreened(), c.isPepScreened(), c.getDecisionReason(),
                c.getSubmittedAt(), c.getReviewedAt());
    }

    private KybDocumentResponse toDocResponse(KybDocument d) {
        return new KybDocumentResponse(d.getId(), d.getMerchantId(), d.getKybCaseId(), d.getDocumentType(),
                d.getFileName(), d.getContentType(), d.getStorageReference(), d.getStatus(), d.getExpiresOn());
    }
}
