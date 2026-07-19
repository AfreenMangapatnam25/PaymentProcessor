package com.paymentprocessor.disputeservice.service;

import com.paymentprocessor.disputeservice.domain.enums.EvidenceStatus;
import com.paymentprocessor.disputeservice.domain.enums.EvidenceType;
import com.paymentprocessor.disputeservice.domain.enums.TimelineEventType;
import com.paymentprocessor.disputeservice.dto.request.ReviewEvidenceRequest;
import com.paymentprocessor.disputeservice.dto.request.UploadEvidenceRequest;
import com.paymentprocessor.disputeservice.entity.Dispute;
import com.paymentprocessor.disputeservice.entity.Evidence;
import com.paymentprocessor.disputeservice.exception.DisputeNotFoundException;
import com.paymentprocessor.disputeservice.exception.EvidenceValidationException;
import com.paymentprocessor.disputeservice.exception.InvalidDisputeStateException;
import com.paymentprocessor.disputeservice.repository.DisputeRepository;
import com.paymentprocessor.disputeservice.repository.EvidenceRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the collection, validation and review of merchant evidence. Enforces
 * the format and size rules, simulates malware scanning / OCR, and records
 * review outcomes used when assembling a representment package.
 */
@Service
public class EvidenceService {

    private final EvidenceRepository repository;
    private final DisputeRepository disputeRepository;
    private final DisputeEventService timeline;

    public EvidenceService(EvidenceRepository repository, DisputeRepository disputeRepository,
                           DisputeEventService timeline) {
        this.repository = repository;
        this.disputeRepository = disputeRepository;
        this.timeline = timeline;
    }

    /**
     * Registers an uploaded evidence document against a dispute after validating
     * its format and size.
     */
    @Transactional
    public Evidence upload(String disputeId, UploadEvidenceRequest request) {
        Dispute dispute = requireDispute(disputeId);
        if (dispute.getStatus().isTerminal()) {
            throw new InvalidDisputeStateException(
                    "Cannot upload evidence to a dispute in terminal state " + dispute.getStatus());
        }

        EvidenceType type = resolveAndValidateType(request.getFileName(), request.getSizeBytes());

        Evidence evidence = new Evidence();
        evidence.setDisputeId(disputeId);
        evidence.setFileName(request.getFileName());
        evidence.setStorageKey(request.getStorageKey());
        evidence.setType(type);
        evidence.setCategory(request.getCategory());
        evidence.setSizeBytes(request.getSizeBytes());
        evidence.setSha256(request.getSha256());
        evidence.setDescription(request.getDescription());
        evidence.setUploadedBy(request.getUploadedBy());
        evidence.setStatus(EvidenceStatus.UPLOADED);
        // Simulated post-upload processing that a real store would perform async.
        evidence.setMalwareScanned(true);
        evidence = repository.save(evidence);

        timeline.record(dispute, TimelineEventType.EVIDENCE_UPLOADED,
                DisputeEventService.ACTOR_MERCHANT,
                "Evidence uploaded: " + request.getFileName()
                        + " (" + request.getCategory() + ")");
        return evidence;
    }

    /**
     * Records a review decision (accept / reject) on a piece of evidence.
     */
    @Transactional
    public Evidence review(String evidenceId, ReviewEvidenceRequest request) {
        Evidence evidence = repository.findById(evidenceId)
                .orElseThrow(() -> new DisputeNotFoundException("Evidence not found: " + evidenceId));
        Dispute dispute = requireDispute(evidence.getDisputeId());

        boolean accepted = Boolean.TRUE.equals(request.getAccepted());
        evidence.setStatus(accepted ? EvidenceStatus.ACCEPTED : EvidenceStatus.REJECTED);
        evidence.setReviewedAt(Instant.now());
        evidence = repository.save(evidence);

        timeline.record(dispute, TimelineEventType.EVIDENCE_REVIEWED,
                request.getReviewer() == null ? DisputeEventService.ACTOR_PLATFORM : request.getReviewer(),
                (accepted ? "Accepted" : "Rejected") + " evidence " + evidence.getFileName()
                        + (request.getNotes() != null ? " — " + request.getNotes() : ""));
        return evidence;
    }

    /** Marks all accepted evidence for a dispute as submitted to the network. */
    @Transactional
    public int markAcceptedAsSubmitted(String disputeId) {
        List<Evidence> accepted = repository.findByDisputeIdAndStatus(disputeId, EvidenceStatus.ACCEPTED);
        Instant now = Instant.now();
        for (Evidence evidence : accepted) {
            evidence.setStatus(EvidenceStatus.SUBMITTED);
            evidence.setSubmittedAt(now);
        }
        repository.saveAll(accepted);
        return accepted.size();
    }

    @Transactional(readOnly = true)
    public List<Evidence> forDispute(String disputeId) {
        return repository.findByDisputeId(disputeId);
    }

    @Transactional(readOnly = true)
    public List<Evidence> acceptedForDispute(String disputeId) {
        return repository.findByDisputeIdAndStatus(disputeId, EvidenceStatus.ACCEPTED);
    }

    @Transactional(readOnly = true)
    public Evidence get(String evidenceId) {
        return repository.findById(evidenceId)
                .orElseThrow(() -> new DisputeNotFoundException("Evidence not found: " + evidenceId));
    }

    // ----- validation ---------------------------------------------------------

    private EvidenceType resolveAndValidateType(String fileName, Long sizeBytes) {
        String extension = extensionOf(fileName);
        EvidenceType type = EvidenceType.forExtension(extension);
        if (type == null) {
            throw new EvidenceValidationException("Unsupported evidence file type: " + extension);
        }
        if (sizeBytes != null && sizeBytes > type.getMaxSizeBytes()) {
            throw new EvidenceValidationException(
                    "Evidence exceeds the " + type + " size limit of "
                            + type.getMaxSizeBytes() + " bytes");
        }
        return type;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1) : "";
    }

    private Dispute requireDispute(String disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException("Dispute not found: " + disputeId));
    }
}
