package com.paymentprocessor.disputeservice.service;

import com.paymentprocessor.disputeservice.config.NetworkRules;
import com.paymentprocessor.disputeservice.domain.enums.DisputeStage;
import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.domain.enums.EvidenceCategory;
import com.paymentprocessor.disputeservice.domain.enums.RepresentmentStatus;
import com.paymentprocessor.disputeservice.domain.enums.TimelineEventType;
import com.paymentprocessor.disputeservice.dto.request.ArbitrationDecisionRequest;
import com.paymentprocessor.disputeservice.dto.request.ArbitrationFilingRequest;
import com.paymentprocessor.disputeservice.dto.request.IssuerResponseRequest;
import com.paymentprocessor.disputeservice.dto.request.SubmitRepresentmentRequest;
import com.paymentprocessor.disputeservice.entity.Dispute;
import com.paymentprocessor.disputeservice.entity.Evidence;
import com.paymentprocessor.disputeservice.entity.Representment;
import com.paymentprocessor.disputeservice.exception.DeadlineExceededException;
import com.paymentprocessor.disputeservice.exception.EvidenceValidationException;
import com.paymentprocessor.disputeservice.exception.InvalidDisputeStateException;
import com.paymentprocessor.disputeservice.integration.LedgerClient;
import com.paymentprocessor.disputeservice.repository.RepresentmentRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles representments, pre-arbitration and arbitration: assembling accepted
 * evidence into a network package, validating deadline and completeness rules,
 * submitting to the network, and recording issuer / network decisions which
 * drive the dispute to resolution.
 */
@Service
public class RepresentmentService {

    private static final Logger log = LoggerFactory.getLogger(RepresentmentService.class);

    private final RepresentmentRepository repository;
    private final DisputeService disputeService;
    private final EvidenceService evidenceService;
    private final DisputeEventService timeline;
    private final ReasonCodeCatalogService reasonCodeCatalogService;
    private final NetworkRules networkRules;
    private final LedgerClient ledgerClient;

    public RepresentmentService(RepresentmentRepository repository, DisputeService disputeService,
                                EvidenceService evidenceService, DisputeEventService timeline,
                                ReasonCodeCatalogService reasonCodeCatalogService,
                                NetworkRules networkRules, LedgerClient ledgerClient) {
        this.repository = repository;
        this.disputeService = disputeService;
        this.evidenceService = evidenceService;
        this.timeline = timeline;
        this.reasonCodeCatalogService = reasonCodeCatalogService;
        this.networkRules = networkRules;
        this.ledgerClient = ledgerClient;
    }

    /**
     * Assembles accepted evidence and submits a representment to the network.
     * Enforces the deadline and reason-code evidence-completeness rules; a
     * representment must cover the full disputed amount.
     */
    @Transactional
    public Representment submit(String disputeId, SubmitRepresentmentRequest request) {
        Dispute dispute = disputeService.get(disputeId);
        if (dispute.getStatus() != DisputeStatus.EVIDENCE_REVIEW) {
            throw new InvalidDisputeStateException(
                    "Representment requires status EVIDENCE_REVIEW but was " + dispute.getStatus());
        }
        if (dispute.isPastDeadline(Instant.now())) {
            throw new DeadlineExceededException(
                    "Response deadline " + dispute.getDeadlineAt() + " has passed for dispute " + disputeId);
        }

        List<Evidence> accepted = evidenceService.acceptedForDispute(disputeId);
        if (accepted.isEmpty()) {
            throw new EvidenceValidationException(
                    "No accepted evidence to submit for dispute " + disputeId);
        }
        assertPackageComplete(dispute, accepted);

        long fee = networkRules.representmentFeeMinor(dispute.getNetwork());
        Representment representment = new Representment();
        representment.setDisputeId(disputeId);
        representment.setStage(DisputeStage.REPRESENTMENT);
        representment.setStatus(RepresentmentStatus.SUBMITTED);
        representment.setNetworkReference(newNetworkReference());
        representment.setEvidenceCount(accepted.size());
        representment.setFeeMinor(fee);
        representment.setNarrative(request.getNarrative());
        representment.setSubmittedBy(request.getSubmittedBy());
        representment.setSubmittedAt(Instant.now());
        representment = repository.save(representment);

        evidenceService.markAcceptedAsSubmitted(disputeId);
        ledgerClient.postFee(disputeId, fee, dispute.getCurrency(), "Representment fee");

        disputeService.markRepresented(disputeId);
        timeline.record(dispute, TimelineEventType.REPRESENTMENT_SUBMITTED, DisputeEventService.ACTOR_PLATFORM,
                "Representment submitted with " + accepted.size() + " documents, reference "
                        + representment.getNetworkReference());
        log.info("Submitted representment {} for dispute {}", representment.getId(), disputeId);
        return representment;
    }

    /**
     * Records the issuer's decision on a representment and drives the dispute to
     * its next state (won, or pre-arbitration on rejection / escalation).
     */
    @Transactional
    public Dispute recordIssuerResponse(String disputeId, IssuerResponseRequest request) {
        Dispute dispute = disputeService.get(disputeId);
        updateLatestDecision(disputeId, DisputeStage.REPRESENTMENT, request.getResponse().name());
        timeline.record(dispute, TimelineEventType.ISSUER_RESPONSE_RECEIVED, DisputeEventService.ACTOR_NETWORK,
                "Issuer response: " + request.getResponse()
                        + (request.getNotes() != null ? " — " + request.getNotes() : ""));

        return switch (request.getResponse()) {
            case ACCEPTED -> disputeService.markWon(disputeId, DisputeEventService.ACTOR_NETWORK);
            case REJECTED, ESCALATED -> disputeService.moveToPreArbitration(disputeId);
        };
    }

    /**
     * Escalates a rejected dispute to network arbitration, posting the filing fee.
     */
    @Transactional
    public Representment fileArbitration(String disputeId, ArbitrationFilingRequest request) {
        Dispute dispute = disputeService.get(disputeId);
        if (dispute.getStatus() != DisputeStatus.PRE_ARBITRATION) {
            throw new InvalidDisputeStateException(
                    "Arbitration filing requires status PRE_ARBITRATION but was " + dispute.getStatus());
        }

        disputeService.moveToArbitration(disputeId);

        long fee = networkRules.arbitrationFilingFeeMinor(dispute.getNetwork());
        Representment filing = new Representment();
        filing.setDisputeId(disputeId);
        filing.setStage(DisputeStage.ARBITRATION);
        filing.setStatus(RepresentmentStatus.SUBMITTED);
        filing.setNetworkReference(newNetworkReference());
        filing.setFeeMinor(fee);
        filing.setNarrative(request.getNarrative());
        filing.setSubmittedBy(request.getFiledBy());
        filing.setSubmittedAt(Instant.now());
        filing = repository.save(filing);

        if (fee > 0) {
            ledgerClient.postFee(disputeId, fee, dispute.getCurrency(), "Arbitration filing fee");
        }
        timeline.record(dispute, TimelineEventType.ARBITRATION_FILED, DisputeEventService.ACTOR_PLATFORM,
                "Arbitration filed, reference " + filing.getNetworkReference()
                        + ", filing fee " + fee + " " + dispute.getCurrency());
        return filing;
    }

    /**
     * Records the network's binding arbitration decision and resolves the dispute.
     */
    @Transactional
    public Dispute recordArbitrationDecision(String disputeId, ArbitrationDecisionRequest request) {
        Dispute dispute = disputeService.get(disputeId);
        if (dispute.getStatus() != DisputeStatus.ARBITRATION) {
            throw new InvalidDisputeStateException(
                    "Arbitration decision requires status ARBITRATION but was " + dispute.getStatus());
        }
        updateLatestDecision(disputeId, DisputeStage.ARBITRATION, request.getOutcome().name());
        timeline.record(dispute, TimelineEventType.ARBITRATION_DECISION, DisputeEventService.ACTOR_NETWORK,
                "Arbitration decision: " + request.getOutcome());

        return switch (request.getOutcome()) {
            case WON -> disputeService.markWon(disputeId, DisputeEventService.ACTOR_NETWORK);
            case LOST -> disputeService.markLost(disputeId, DisputeEventService.ACTOR_NETWORK,
                    "Arbitration decided against merchant");
        };
    }

    @Transactional(readOnly = true)
    public List<Representment> forDispute(String disputeId) {
        return repository.findByDisputeId(disputeId);
    }

    // ----- internals ----------------------------------------------------------

    private void assertPackageComplete(Dispute dispute, List<Evidence> accepted) {
        Set<EvidenceCategory> present = EnumSet.noneOf(EvidenceCategory.class);
        for (Evidence evidence : accepted) {
            if (evidence.getCategory() != null) {
                present.add(evidence.getCategory());
            }
        }
        Set<EvidenceCategory> missing = reasonCodeCatalogService.missingCategories(
                dispute.getNetwork(), dispute.getReasonCode(), present);
        if (!missing.isEmpty()) {
            throw new EvidenceValidationException(
                    "Evidence package is missing required categories for reason "
                            + dispute.getReasonCode() + ": " + missing);
        }
    }

    private void updateLatestDecision(String disputeId, DisputeStage stage, String decision) {
        repository.findFirstByDisputeIdAndStageOrderByCreatedAtDesc(disputeId, stage)
                .ifPresent(representment -> {
                    representment.setDecidedAt(Instant.now());
                    if (stage == DisputeStage.REPRESENTMENT) {
                        representment.setStatus("ACCEPTED".equals(decision)
                                ? RepresentmentStatus.ACCEPTED : RepresentmentStatus.REJECTED);
                        representment.setIssuerResponse(
                                com.paymentprocessor.disputeservice.domain.enums.IssuerResponse.valueOf(decision));
                    }
                    repository.save(representment);
                });
    }

    private static String newNetworkReference() {
        return "NET-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}
