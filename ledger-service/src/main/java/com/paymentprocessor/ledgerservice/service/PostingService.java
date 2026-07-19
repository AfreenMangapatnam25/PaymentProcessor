package com.paymentprocessor.ledgerservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import com.paymentprocessor.ledgerservice.domain.enums.JournalStatus;
import com.paymentprocessor.ledgerservice.domain.enums.NormalBalance;
import com.paymentprocessor.ledgerservice.domain.enums.ReversalReason;
import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.entity.AccountBalance;
import com.paymentprocessor.ledgerservice.entity.AccountType;
import com.paymentprocessor.ledgerservice.entity.Entry;
import com.paymentprocessor.ledgerservice.entity.Journal;
import com.paymentprocessor.ledgerservice.event.BalanceUpdatedEvent;
import com.paymentprocessor.ledgerservice.event.LedgerPostedEvent;
import com.paymentprocessor.ledgerservice.repository.AccountBalanceRepository;
import com.paymentprocessor.ledgerservice.repository.AccountRepository;
import com.paymentprocessor.ledgerservice.repository.AccountTypeRepository;
import com.paymentprocessor.ledgerservice.repository.EntryRepository;
import com.paymentprocessor.ledgerservice.repository.JournalRepository;
import com.paymentprocessor.ledgerservice.support.Ids;
import com.paymentprocessor.ledgerservice.web.dto.JournalLineRequest;
import com.paymentprocessor.ledgerservice.web.dto.PostJournalRequest;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import com.paymentprocessor.ledgerservice.web.error.UnbalancedJournalException;
import com.paymentprocessor.ledgerservice.web.error.UnprocessableException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The posting engine: the only component permitted to write journals, entries,
 * and balance mutations. Every posting is atomic, balanced, immutable, and
 * idempotent (README §Posting Engine).
 */
@Service
public class PostingService {

    private static final Logger log = LoggerFactory.getLogger(PostingService.class);

    private final JournalRepository journals;
    private final EntryRepository entries;
    private final AccountRepository accounts;
    private final AccountTypeRepository accountTypes;
    private final AccountBalanceRepository balances;
    private final AccountingPeriodService periods;
    private final OutboxService outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PostingService(JournalRepository journals,
                          EntryRepository entries,
                          AccountRepository accounts,
                          AccountTypeRepository accountTypes,
                          AccountBalanceRepository balances,
                          AccountingPeriodService periods,
                          OutboxService outbox,
                          ObjectMapper objectMapper,
                          Clock clock) {
        this.journals = journals;
        this.entries = entries;
        this.accounts = accounts;
        this.accountTypes = accountTypes;
        this.balances = balances;
        this.periods = periods;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** A validated, normalised posting line used internally by the engine. */
    public record LineSpec(String accountId, EntryDirection direction, long amountMinor,
                           String currency, String description) {
    }

    /** Public API: post a balanced journal from a client request. */
    @Transactional
    public Journal post(PostJournalRequest req) {
        List<LineSpec> lines = new ArrayList<>();
        for (JournalLineRequest line : req.lines()) {
            lines.add(new LineSpec(line.accountId(), line.direction(), line.amountMinor(),
                    line.currency(), line.description()));
        }
        Instant effectiveAt = req.effectiveAt() != null ? req.effectiveAt() : Instant.now(clock);
        return postPrepared(req.eventType(), req.externalRef(), req.idempotencyKey(), req.description(),
                toJson(req.metadata()), effectiveAt, req.createdBy(), null, null, lines);
    }

    /**
     * Core posting routine shared by ordinary postings and reversals. Validates,
     * persists the journal and its immutable entries, updates balances under a
     * pessimistic lock, and enqueues domain events to the outbox.
     */
    @Transactional
    public Journal postPrepared(String eventType, String externalRef, String idempotencyKey,
                                String description, String metadataJson, Instant effectiveAt,
                                String createdBy, String reversesJournalId, ReversalReason reversalReason,
                                List<LineSpec> lines) {

        // 1. Idempotency: return the existing journal for a repeated key.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = journals.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.debug("Idempotent replay for key {} -> journal {}", idempotencyKey, existing.get().getId());
                return existing.get();
            }
        }
        if (lines == null || lines.size() < 2) {
            throw new UnbalancedJournalException("A journal must contain at least two lines");
        }

        // 2. Validate accounts and normalise per-line currency; 3. balance check.
        Map<String, Account> accountCache = new LinkedHashMap<>();
        Map<String, long[]> byCurrency = new LinkedHashMap<>(); // currency -> [debit, credit]
        long totalDebit = 0;
        long totalCredit = 0;
        for (LineSpec line : lines) {
            if (line.amountMinor() <= 0) {
                throw new UnbalancedJournalException("Line amounts must be positive");
            }
            Account account = accountCache.computeIfAbsent(line.accountId(), id ->
                    accounts.findById(id).orElseThrow(() -> NotFoundException.of("Account", id)));
            if (!account.isActive()) {
                throw new UnprocessableException("ACCOUNT_INACTIVE",
                        "Account " + account.getId() + " is not active");
            }
            String currency = line.currency() != null ? line.currency() : account.getCurrency();
            if (!currency.equals(account.getCurrency())) {
                throw new UnprocessableException("CURRENCY_MISMATCH",
                        "Line currency " + currency + " does not match account " + account.getId()
                                + " currency " + account.getCurrency());
            }
            long[] sums = byCurrency.computeIfAbsent(currency, c -> new long[2]);
            if (line.direction() == EntryDirection.DEBIT) {
                sums[0] += line.amountMinor();
                totalDebit += line.amountMinor();
            } else {
                sums[1] += line.amountMinor();
                totalCredit += line.amountMinor();
            }
        }
        for (Map.Entry<String, long[]> e : byCurrency.entrySet()) {
            if (e.getValue()[0] != e.getValue()[1]) {
                throw new UnbalancedJournalException(String.format(
                        "Debits (%d) do not equal credits (%d) for currency %s",
                        e.getValue()[0], e.getValue()[1], e.getKey()));
            }
        }

        // 4. Resolve the accounting period (may be null; throws if closed/locked).
        String periodId = periods.resolvePostablePeriodId(effectiveAt);

        // 5. Persist the journal.
        Instant now = Instant.now(clock);
        Journal journal = new Journal();
        journal.setId(Ids.journalId(now));
        journal.setEventType(eventType);
        journal.setExternalRef(externalRef);
        journal.setIdempotencyKey(idempotencyKey);
        journal.setDescription(description);
        journal.setMetadata(metadataJson);
        journal.setStatus(JournalStatus.POSTED);
        journal.setReversesJournalId(reversesJournalId);
        journal.setReversalReason(reversalReason);
        journal.setPeriodId(periodId);
        journal.setEffectiveAt(effectiveAt);
        journal.setPostedAt(now);
        journal.setCreatedBy(createdBy);
        journals.save(journal);

        // 6. Persist the immutable entry lines.
        List<Entry> savedEntries = new ArrayList<>();
        int lineNumber = 1;
        for (LineSpec line : lines) {
            Account account = accountCache.get(line.accountId());
            Entry entry = new Entry();
            entry.setJournalId(journal.getId());
            entry.setLineNumber(lineNumber++);
            entry.setAccountId(line.accountId());
            entry.setDirection(line.direction());
            entry.setAmountMinor(line.amountMinor());
            entry.setCurrency(account.getCurrency());
            entry.setDescription(line.description());
            entry.setEffectiveAt(effectiveAt);
            savedEntries.add(entries.save(entry));
        }

        // 7. Update balances per affected account under a pessimistic lock.
        List<String> affected = new ArrayList<>(accountCache.keySet());
        for (String accountId : affected) {
            Account account = accountCache.get(accountId);
            NormalBalance nb = normalBalanceOf(account);
            boolean debitPositive = nb == NormalBalance.DEBIT;

            long delta = 0;
            long maxEntryId = 0;
            for (Entry entry : savedEntries) {
                if (!entry.getAccountId().equals(accountId)) {
                    continue;
                }
                long debitSigned = entry.getDirection() == EntryDirection.DEBIT
                        ? entry.getAmountMinor() : -entry.getAmountMinor();
                delta += debitPositive ? debitSigned : -debitSigned;
                maxEntryId = Math.max(maxEntryId, entry.getId());
            }

            AccountBalance balance = balances.findByIdForUpdate(accountId)
                    .orElseGet(() -> initBalance(accountId, account.getCurrency(), now));
            balance.setPostedMinor(balance.getPostedMinor() + delta);
            balance.setEntryHighWater(Math.max(balance.getEntryHighWater(), maxEntryId));
            balance.recomputeAvailable();
            balance.setUpdatedAt(now);
            balances.save(balance);

            outbox.append("Account", accountId, BalanceUpdatedEvent.TYPE, new BalanceUpdatedEvent(
                    accountId, balance.getCurrency(), balance.getPostedMinor(), balance.getHeldMinor(),
                    balance.getAvailableMinor(), balance.getEntryHighWater(), now));
        }

        // 8. Enqueue the LedgerPosted event.
        outbox.append("Journal", journal.getId(), LedgerPostedEvent.TYPE, new LedgerPostedEvent(
                journal.getId(), eventType, externalRef, now, affected, totalDebit, totalCredit));

        log.info("Posted journal {} ({} lines, {} accounts, {} minor)",
                journal.getId(), savedEntries.size(), affected.size(), totalDebit);
        return journal;
    }

    private AccountBalance initBalance(String accountId, String currency, Instant now) {
        AccountBalance balance = new AccountBalance();
        balance.setAccountId(accountId);
        balance.setCurrency(currency);
        balance.setPostedMinor(0L);
        balance.setPendingMinor(0L);
        balance.setHeldMinor(0L);
        balance.setAvailableMinor(0L);
        balance.setEntryHighWater(0L);
        balance.setUpdatedAt(now);
        return balance;
    }

    private NormalBalance normalBalanceOf(Account account) {
        AccountType type = accountTypes.findById(account.getTypeCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Account " + account.getId() + " references unknown type " + account.getTypeCode()));
        return type.getNormalBalance();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise journal metadata", e);
        }
    }
}
