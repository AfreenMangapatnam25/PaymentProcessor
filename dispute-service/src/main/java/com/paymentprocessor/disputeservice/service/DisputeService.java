package com.paymentprocessor.disputeservice.service;

import com.paymentprocessor.disputeservice.config.NetworkRules;
import com.paymentprocessor.disputeservice.domain.DisputeStateMachine;
import com.paymentprocessor.disputeservice.domain.enums.DisputeStage;
import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.domain.enums.DisputeType;
import com.paymentprocessor.disputeservice.domain.enums.LiabilityParty;
import com.paymentprocessor.disputeservice.domain.enums.NotificationChannel;
import com.paymentprocessor.disputeservice.domain.enums.NotificationUrgency;
import com.paymentprocessor.disputeservice.domain.enums.TimelineEventType;
import com.paymentprocessor.disputeservice.dto.request.CreateDisputeRequest;
import com.paymentprocessor.disputeservice.entity.Dispute;
import com.paymentprocessor.disputeservice.entity.Liability;
import com.paymentprocessor.disputeservice.event.ChargebackReceivedEvent;
import com.paymentprocessor.disputeservice.event.DisputeCreatedEvent;
import com.paymentprocessor.disputeservice.event.DisputeEventPublisher;
import com.paymentprocessor.disputeservice.event.DisputeLostEvent;
import com.paymentprocessor.disputeservice.event.DisputeWonEvent;
import com.paymentprocessor.disputeservice.exception.DisputeNotFoundException;
import com.paymentprocessor.disputeservice.exception.DuplicateDisputeException;
import com.paymentprocessor.disputeservice.integration.NotificationClient;
import com.paymentprocessor.disputeservice.integration.PaymentClient;
import com.paymentprocessor.disputeservice.repository.DisputeRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core lifecycle orchestrator for disputes. Owns creation from an inbound
 * chargeback, all status transitions (guarded by {@link DisputeStateMachine}),
 * deadline computation, merchant notification and the coordination of financial
 * postings and domain-event publication.
 */
@Service
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    /** States in which the merchant / platform still owes the network a response. */
    public static final Set<DisputeStatus> AWAITING_RESPONSE = Set.of(
            DisputeStatus.OPEN, DisputeStatus.PENDING_EVIDENCE,
            DisputeStatus.EVIDENCE_REVIEW, DisputeStatus.PRE_ARBITRATION);

    private final DisputeRepository repository;
    private final DisputeEventService timeline;
    private final LiabilityService liabilityService;
    private final NotificationClient notificationClient;
    private final PaymentClient paymentClient;
    private final DisputeEventPublisher eventPublisher;
    private final NetworkRules networkRules;

    public DisputeService(DisputeRepository repository, DisputeEventService timeline,
                          LiabilityService liabilityService, NotificationClient notificationClient,
                          PaymentClient paymentClient, DisputeEventPublisher eventPublisher,
                          NetworkRules networkRules) {
        this.repository = repository;
        this.timeline = timeline;
        this.liabilityService = liabilityService;
        this.notificationClient = notificationClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
        this.networkRules = networkRules;
    }

    // ----- creation -----------------------------------------------------------

    /**
     * Opens a new dispute from an inbound chargeback / retrieval notification:
     * validates uniqueness, computes the network deadline, records the financial
     * impact, flags the transaction, notifies the merchant and publishes the
     * creation events.
     */
    @Transactional
    public Dispute createFromChargeback(CreateDisputeRequest request) {
        if (repository.existsByChargebackId(request.getChargebackId())) {
            throw new DuplicateDisputeException(
                    "Dispute already exists for chargeback " + request.getChargebackId());
        }

        Instant now = Instant.now();
        DisputeStage stage = request.getType() == DisputeType.RETRIEVAL_REQUEST
                ? DisputeStage.RETRIEVAL : DisputeStage.CHARGEBACK;
        long feeMinor = networkRules.chargebackFeeMinor(request.getNetwork());

        Dispute dispute = new Dispute();
        dispute.setChargebackId(request.getChargebackId());
        dispute.setTransactionId(request.getTransactionId());
        dispute.setPaymentId(request.getPaymentId());
        dispute.setMerchantId(request.getMerchantId());
        dispute.setCustomerId(request.getCustomerId());
        dispute.setNetwork(request.getNetwork());
        dispute.setType(request.getType());
        dispute.setSource(request.getSource());
        dispute.setStage(stage);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setReasonCode(request.getReasonCode());
        dispute.setReasonDescription(request.getReasonDescription());
        dispute.setAmountMinor(request.getAmountMinor());
        dispute.setCurrency(request.getCurrency());
        dispute.setChargebackFeeMinor(feeMinor);
        dispute.setPartial(request.isPartial());
        dispute.setLiabilityParty(LiabilityParty.PENDING);
        dispute.setReceivedAt(now);
        dispute.setOpenedAt(now);
        dispute.setDeadlineAt(computeDeadline(now, request.getNetwork(), stage));
        dispute = repository.save(dispute);

        timeline.record(dispute, TimelineEventType.DISPUTE_CREATED, DisputeEventService.ACTOR_SYSTEM,
                "Dispute created from " + request.getNetwork() + " " + request.getType()
                        + " (reason " + request.getReasonCode() + ")");

        // Financial impact.
        Liability liability = liabilityService.recordChargebackImpact(
                dispute, request.getChargebackRatePercent());
        timeline.record(dispute, TimelineEventType.LEDGER_POSTED, DisputeEventService.ACTOR_SYSTEM,
                "Chargeback debit posted, ledger journal " + liability.getLedgerJournalId());

        // Flag the original transaction in the Payment Service.
        if (dispute.getTransactionId() != null) {
            paymentClient.flagTransactionDisputed(dispute.getTransactionId(), dispute.getId());
        }

        notifyMerchant(dispute, "Chargeback received",
                "A chargeback has been raised on " + request.getNetwork()
                        + " for " + formatAmount(dispute) + ". Respond by " + dispute.getDeadlineAt() + ".",
                NotificationUrgency.IMMEDIATE,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD,
                        NotificationChannel.WEBHOOK));

        // Domain events for downstream consumers.
        eventPublisher.publish(new DisputeCreatedEvent(dispute.getId(), dispute.getMerchantId(),
                dispute.getChargebackId(), dispute.getNetwork(), dispute.getReasonCode(),
                dispute.getAmountMinor(), dispute.getCurrency(), dispute.getDeadlineAt(), now));
        eventPublisher.publish(new ChargebackReceivedEvent(dispute.getId(), dispute.getMerchantId(),
                dispute.getNetwork(), dispute.getAmountMinor(), feeMinor, dispute.getCurrency(),
                liability.getLedgerJournalId(), now));

        log.info("Opened dispute {} for chargeback {} (merchant {})",
                dispute.getId(), dispute.getChargebackId(), dispute.getMerchantId());
        return dispute;
    }

    // ----- lifecycle transitions ---------------------------------------------

    /** Requests evidence from the merchant (OPEN -> PENDING_EVIDENCE). */
    @Transactional
    public Dispute requestEvidence(String disputeId) {
        Dispute dispute = require(disputeId);
        transition(dispute, DisputeStatus.PENDING_EVIDENCE, DisputeEventService.ACTOR_SYSTEM,
                "Evidence requested from merchant");
        timeline.record(dispute, TimelineEventType.EVIDENCE_REQUESTED, DisputeEventService.ACTOR_SYSTEM,
                "Merchant asked to upload evidence");
        notifyMerchant(dispute, "Evidence requested",
                "Please upload evidence for dispute " + dispute.getId() + " before " + dispute.getDeadlineAt(),
                NotificationUrgency.HIGH, Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD));
        return dispute;
    }

    /** Moves a dispute into review once evidence has been supplied. */
    @Transactional
    public Dispute markEvidenceUnderReview(String disputeId) {
        Dispute dispute = require(disputeId);
        transition(dispute, DisputeStatus.EVIDENCE_REVIEW, DisputeEventService.ACTOR_PLATFORM,
                "Evidence received; under review");
        return dispute;
    }

    /** Transitions to REPRESENTED after a package is submitted (used by RepresentmentService). */
    @Transactional
    public Dispute markRepresented(String disputeId) {
        Dispute dispute = require(disputeId);
        transition(dispute, DisputeStatus.REPRESENTED, DisputeEventService.ACTOR_PLATFORM,
                "Representment submitted to network");
        return dispute;
    }

    /** Escalates to pre-arbitration after an issuer rejection, resetting the deadline. */
    @Transactional
    public Dispute moveToPreArbitration(String disputeId) {
        Dispute dispute = require(disputeId);
        transition(dispute, DisputeStatus.PRE_ARBITRATION, DisputeEventService.ACTOR_NETWORK,
                "Issuer rejected representment; pre-arbitration opened");
        dispute.setStage(DisputeStage.PRE_ARBITRATION);
        dispute.setDeadlineAt(computeDeadline(Instant.now(), dispute.getNetwork(), DisputeStage.PRE_ARBITRATION));
        timeline.record(dispute, TimelineEventType.PRE_ARBITRATION_RECEIVED, DisputeEventService.ACTOR_NETWORK,
                "Pre-arbitration notice received");
        notifyMerchant(dispute, "Pre-arbitration required",
                "The issuer rejected the representment for dispute " + dispute.getId()
                        + ". A pre-arbitration response is required by " + dispute.getDeadlineAt(),
                NotificationUrgency.HIGH,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD, NotificationChannel.PHONE));
        return repository.save(dispute);
    }

    /** Escalates to arbitration (used by RepresentmentService when filing). */
    @Transactional
    public Dispute moveToArbitration(String disputeId) {
        Dispute dispute = require(disputeId);
        transition(dispute, DisputeStatus.ARBITRATION, DisputeEventService.ACTOR_PLATFORM,
                "Case escalated to network arbitration");
        dispute.setStage(DisputeStage.ARBITRATION);
        dispute.setDeadlineAt(computeDeadline(Instant.now(), dispute.getNetwork(), DisputeStage.ARBITRATION));
        return repository.save(dispute);
    }

    /** Merchant / platform accepts liability without fighting. */
    @Transactional
    public Dispute acceptLiability(String disputeId, String actor) {
        Dispute dispute = require(disputeId);
        transition(dispute, DisputeStatus.ACCEPTED, actor == null ? DisputeEventService.ACTOR_MERCHANT : actor,
                "Liability accepted without representment");
        dispute.setLiabilityParty(LiabilityParty.MERCHANT);
        dispute.setResolvedAt(Instant.now());
        timeline.record(dispute, TimelineEventType.DISPUTE_ACCEPTED, actor, "Liability accepted");
        liabilityService.finalizeLoss(dispute);
        publishLost(dispute, "Liability accepted");
        return close(dispute.getId());
    }

    /** Resolves a dispute in the merchant's favour: reverse funds and close. */
    @Transactional
    public Dispute markWon(String disputeId, String actor) {
        Dispute dispute = require(disputeId);
        transition(dispute, DisputeStatus.WON, actor == null ? DisputeEventService.ACTOR_NETWORK : actor,
                "Dispute won; chargeback reversed");
        dispute.setLiabilityParty(LiabilityParty.PLATFORM);
        dispute.setResolvedAt(Instant.now());
        String reversalJournal = liabilityService.reverseOnWin(dispute);
        if (dispute.getTransactionId() != null) {
            paymentClient.clearDisputedFlag(dispute.getTransactionId(), dispute.getId());
        }
        timeline.record(dispute, TimelineEventType.DISPUTE_WON, actor,
                "Chargeback reversed, funds returned (reversal " + reversalJournal + ")");
        notifyMerchant(dispute, "Dispute won",
                "Good news — dispute " + dispute.getId() + " was resolved in your favour.",
                NotificationUrgency.STANDARD,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD, NotificationChannel.WEBHOOK));
        eventPublisher.publish(new DisputeWonEvent(dispute.getId(), dispute.getMerchantId(),
                dispute.totalExposureMinor(), dispute.getCurrency(), reversalJournal, Instant.now()));
        return close(dispute.getId());
    }

    /** Resolves a dispute against the merchant: finalise the loss and close. */
    @Transactional
    public Dispute markLost(String disputeId, String actor, String reason) {
        Dispute dispute = require(disputeId);
        transition(dispute, DisputeStatus.LOST, actor == null ? DisputeEventService.ACTOR_NETWORK : actor,
                "Dispute lost: " + reason);
        dispute.setLiabilityParty(LiabilityParty.MERCHANT);
        dispute.setResolvedAt(Instant.now());
        liabilityService.finalizeLoss(dispute);
        timeline.record(dispute, TimelineEventType.DISPUTE_LOST, actor, "Chargeback stands: " + reason);
        notifyMerchant(dispute, "Dispute lost",
                "Dispute " + dispute.getId() + " was resolved against you. " + reason,
                NotificationUrgency.STANDARD,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD, NotificationChannel.WEBHOOK));
        publishLost(dispute, reason);
        return close(dispute.getId());
    }

    /**
     * Auto-loses a dispute whose response deadline has passed. Invoked by the
     * deadline monitor. No-op if the dispute is no longer awaiting a response.
     */
    @Transactional
    public void autoLoseOnMissedDeadline(String disputeId) {
        Dispute dispute = require(disputeId);
        if (!AWAITING_RESPONSE.contains(dispute.getStatus())) {
            return;
        }
        timeline.record(dispute, TimelineEventType.DEADLINE_MISSED, DisputeEventService.ACTOR_SYSTEM,
                "Response deadline missed; dispute auto-lost");
        markLost(disputeId, DisputeEventService.ACTOR_SYSTEM, "Response deadline missed");
    }

    /** Transitions any resolved dispute to the terminal CLOSED state. */
    @Transactional
    public Dispute close(String disputeId) {
        Dispute dispute = require(disputeId);
        if (dispute.getStatus() == DisputeStatus.CLOSED) {
            return dispute;
        }
        transition(dispute, DisputeStatus.CLOSED, DisputeEventService.ACTOR_SYSTEM, "Dispute closed");
        dispute.setClosedAt(Instant.now());
        return repository.save(dispute);
    }

    // ----- queries ------------------------------------------------------------

    @Transactional(readOnly = true)
    public Dispute get(String disputeId) {
        return require(disputeId);
    }

    @Transactional(readOnly = true)
    public List<Dispute> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Dispute> findByMerchant(String merchantId) {
        return repository.findByMerchantId(merchantId);
    }

    // ----- internals ----------------------------------------------------------

    /**
     * Applies a guarded status transition and records it on the timeline.
     */
    private void transition(Dispute dispute, DisputeStatus target, String actor, String description) {
        DisputeStatus from = dispute.getStatus();
        DisputeStateMachine.assertCanTransition(from, target);
        dispute.setStatus(target);
        repository.save(dispute);
        timeline.recordStatusChange(dispute, from, target, actor, description);
    }

    private void notifyMerchant(Dispute dispute, String subject, String body,
                                NotificationUrgency urgency, Set<NotificationChannel> channels) {
        notificationClient.notifyMerchant(dispute.getMerchantId(), dispute.getId(), subject, body,
                urgency, channels);
        dispute.setMerchantNotified(true);
        repository.save(dispute);
        timeline.record(dispute, TimelineEventType.MERCHANT_NOTIFIED, DisputeEventService.ACTOR_SYSTEM,
                "Merchant notified: " + subject);
    }

    private void publishLost(Dispute dispute, String reason) {
        eventPublisher.publish(new DisputeLostEvent(dispute.getId(), dispute.getMerchantId(),
                dispute.totalExposureMinor(), dispute.getCurrency(), reason, Instant.now()));
    }

    private Instant computeDeadline(Instant base, com.paymentprocessor.disputeservice.domain.enums.Network network,
                                    DisputeStage stage) {
        return base.plus(networkRules.responseDays(network, stage), ChronoUnit.DAYS);
    }

    private static String formatAmount(Dispute dispute) {
        return String.format("%.2f %s", dispute.getAmountMinor() / 100.0, dispute.getCurrency());
    }

    private Dispute require(String disputeId) {
        return repository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException("Dispute not found: " + disputeId));
    }
}
