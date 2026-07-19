package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.JournalStatus;
import com.paymentprocessor.ledgerservice.entity.Entry;
import com.paymentprocessor.ledgerservice.entity.Journal;
import com.paymentprocessor.ledgerservice.event.LedgerReversedEvent;
import com.paymentprocessor.ledgerservice.repository.EntryRepository;
import com.paymentprocessor.ledgerservice.repository.JournalRepository;
import com.paymentprocessor.ledgerservice.service.PostingService.LineSpec;
import com.paymentprocessor.ledgerservice.web.dto.ReverseJournalRequest;
import com.paymentprocessor.ledgerservice.web.error.ConflictException;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import com.paymentprocessor.ledgerservice.web.error.UnprocessableException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrects a posted journal by generating a compensating (reversal) journal.
 * The original journal is never modified except to record that it has been
 * reversed (README §Reversals).
 */
@Service
public class ReversalService {

    private static final Logger log = LoggerFactory.getLogger(ReversalService.class);

    private final JournalRepository journals;
    private final EntryRepository entries;
    private final PostingService postingService;
    private final OutboxService outbox;
    private final Clock clock;
    private final long approvalThresholdMinor;

    public ReversalService(JournalRepository journals,
                           EntryRepository entries,
                           PostingService postingService,
                           OutboxService outbox,
                           Clock clock,
                           @Value("${ledger.reversal.approval-threshold-minor:100000}") long approvalThresholdMinor) {
        this.journals = journals;
        this.entries = entries;
        this.postingService = postingService;
        this.outbox = outbox;
        this.clock = clock;
        this.approvalThresholdMinor = approvalThresholdMinor;
    }

    @Transactional
    public Journal reverse(String journalId, ReverseJournalRequest req) {
        Journal original = journals.findById(journalId)
                .orElseThrow(() -> NotFoundException.of("Journal", journalId));

        if (original.getStatus() == JournalStatus.REVERSED || original.getReversedByJournalId() != null) {
            throw new ConflictException("ALREADY_REVERSED",
                    "Journal " + journalId + " has already been reversed by " + original.getReversedByJournalId());
        }

        List<Entry> originalEntries = entries.findByJournalIdOrderByLineNumberAsc(journalId);
        if (originalEntries.isEmpty()) {
            throw new UnprocessableException("EMPTY_JOURNAL", "Journal " + journalId + " has no entries to reverse");
        }

        // Approval gate for high-value reversals.
        boolean needsApproval = originalEntries.stream()
                .anyMatch(e -> e.getAmountMinor() > approvalThresholdMinor);
        if (needsApproval && (req.approvedBy() == null || req.approvedBy().isBlank())) {
            throw new UnprocessableException("APPROVAL_REQUIRED",
                    "Reversal exceeds the approval threshold of " + approvalThresholdMinor
                            + " minor units and requires an approver");
        }

        // Build compensating lines with swapped directions.
        List<LineSpec> lines = new ArrayList<>();
        for (Entry e : originalEntries) {
            lines.add(new LineSpec(e.getAccountId(), e.getDirection().opposite(), e.getAmountMinor(),
                    e.getCurrency(), "Reversal of entry " + e.getId()));
        }

        Instant now = Instant.now(clock);
        String description = req.description() != null
                ? req.description()
                : "Reversal of " + journalId + " (" + req.reason() + ")";
        String eventType = original.getEventType() != null ? original.getEventType() + ".REVERSAL" : "REVERSAL";

        Journal reversal = postingService.postPrepared(
                eventType, original.getExternalRef(), req.idempotencyKey(), description,
                original.getMetadata(), now, req.createdBy(), original.getId(), req.reason(), lines);

        // Link the original to its reversal (the only permitted mutation).
        original.setStatus(JournalStatus.REVERSED);
        original.setReversedByJournalId(reversal.getId());
        journals.save(original);

        outbox.append("Journal", reversal.getId(), LedgerReversedEvent.TYPE, new LedgerReversedEvent(
                reversal.getId(), original.getId(), req.reason().name(), now));

        log.info("Reversed journal {} with compensating journal {} (reason {})",
                original.getId(), reversal.getId(), req.reason());
        return reversal;
    }
}
