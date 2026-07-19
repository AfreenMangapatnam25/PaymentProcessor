package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.entity.Entry;
import com.paymentprocessor.ledgerservice.repository.EntryRepository;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only access to individual ledger entries. Entries are immutable and are
 * only ever created by the posting engine.
 */
@Service
@Transactional(readOnly = true)
public class EntryService {

    private final EntryRepository entries;

    public EntryService(EntryRepository entries) {
        this.entries = entries;
    }

    public Entry get(Long id) {
        return entries.findById(id).orElseThrow(() -> NotFoundException.of("Entry", String.valueOf(id)));
    }

    public List<Entry> byJournal(String journalId) {
        return entries.findByJournalIdOrderByLineNumberAsc(journalId);
    }

    public Page<Entry> byAccount(String accountId, Pageable pageable) {
        return entries.findByAccountIdOrderByIdAsc(accountId, pageable);
    }
}
