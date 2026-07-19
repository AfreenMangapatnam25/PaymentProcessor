package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import com.paymentprocessor.ledgerservice.entity.Entry;
import com.paymentprocessor.ledgerservice.entity.Journal;
import com.paymentprocessor.ledgerservice.repository.EntryRepository;
import com.paymentprocessor.ledgerservice.repository.JournalRepository;
import com.paymentprocessor.ledgerservice.web.dto.EntryResponse;
import com.paymentprocessor.ledgerservice.web.dto.JournalResponse;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side access to journals and their entries, plus response mapping.
 * Journals are immutable; there are deliberately no update or delete operations.
 */
@Service
@Transactional(readOnly = true)
public class JournalService {

    private final JournalRepository journals;
    private final EntryRepository entries;

    public JournalService(JournalRepository journals, EntryRepository entries) {
        this.journals = journals;
        this.entries = entries;
    }

    public Journal get(String id) {
        return journals.findById(id).orElseThrow(() -> NotFoundException.of("Journal", id));
    }

    public Page<Journal> list(Pageable pageable) {
        return journals.findAll(pageable);
    }

    public List<Journal> findByExternalRef(String externalRef) {
        return journals.findByExternalRef(externalRef);
    }

    public List<Entry> entriesOf(String journalId) {
        return entries.findByJournalIdOrderByLineNumberAsc(journalId);
    }

    public JournalResponse toResponse(Journal journal) {
        List<Entry> lines = entries.findByJournalIdOrderByLineNumberAsc(journal.getId());
        long totalDebit = 0;
        long totalCredit = 0;
        List<EntryResponse> lineResponses = new ArrayList<>(lines.size());
        for (Entry e : lines) {
            if (e.getDirection() == EntryDirection.DEBIT) {
                totalDebit += e.getAmountMinor();
            } else {
                totalCredit += e.getAmountMinor();
            }
            lineResponses.add(toEntryResponse(e));
        }
        return new JournalResponse(
                journal.getId(),
                journal.getEventType(),
                journal.getExternalRef(),
                journal.getIdempotencyKey(),
                journal.getDescription(),
                journal.getStatus(),
                journal.getReversesJournalId(),
                journal.getReversedByJournalId(),
                journal.getReversalReason(),
                journal.getPeriodId(),
                journal.getEffectiveAt(),
                journal.getPostedAt(),
                journal.getCreatedBy(),
                lineResponses,
                totalDebit,
                totalCredit,
                totalDebit == totalCredit);
    }

    public static EntryResponse toEntryResponse(Entry e) {
        return new EntryResponse(
                e.getId(),
                e.getLineNumber(),
                e.getAccountId(),
                e.getDirection(),
                e.getAmountMinor(),
                e.getCurrency(),
                e.getDescription(),
                e.getEffectiveAt());
    }
}
