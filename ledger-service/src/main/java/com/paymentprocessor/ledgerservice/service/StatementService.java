package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import com.paymentprocessor.ledgerservice.domain.enums.NormalBalance;
import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.entity.AccountType;
import com.paymentprocessor.ledgerservice.entity.Entry;
import com.paymentprocessor.ledgerservice.repository.AccountRepository;
import com.paymentprocessor.ledgerservice.repository.AccountTypeRepository;
import com.paymentprocessor.ledgerservice.repository.EntryRepository;
import com.paymentprocessor.ledgerservice.web.dto.StatementLine;
import com.paymentprocessor.ledgerservice.web.dto.StatementResponse;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces a historical account statement: an opening balance, the ordered
 * entries within a window, and a running balance carried through each line.
 */
@Service
@Transactional(readOnly = true)
public class StatementService {

    private static final Instant BEGINNING = Instant.EPOCH;

    private final EntryRepository entries;
    private final AccountRepository accounts;
    private final AccountTypeRepository accountTypes;
    private final Clock clock;

    public StatementService(EntryRepository entries,
                            AccountRepository accounts,
                            AccountTypeRepository accountTypes,
                            Clock clock) {
        this.entries = entries;
        this.accounts = accounts;
        this.accountTypes = accountTypes;
        this.clock = clock;
    }

    public StatementResponse statement(String accountId, Instant from, Instant to) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> NotFoundException.of("Account", accountId));
        Instant windowStart = from != null ? from : BEGINNING;
        Instant windowEnd = to != null ? to : Instant.now(clock);
        NormalBalance nb = normalBalanceOf(account);

        long opening = openingBalance(accountId, nb, windowStart);
        long running = opening;

        List<Entry> lines = entries
                .findByAccountIdAndEffectiveAtGreaterThanAndEffectiveAtLessThanEqualOrderByIdAsc(
                        accountId, windowStart, windowEnd);
        List<StatementLine> statementLines = new ArrayList<>(lines.size());
        for (Entry e : lines) {
            long debitSigned = e.getDirection() == EntryDirection.DEBIT ? e.getAmountMinor() : -e.getAmountMinor();
            long delta = nb == NormalBalance.DEBIT ? debitSigned : -debitSigned;
            running += delta;
            statementLines.add(new StatementLine(
                    e.getId(), e.getJournalId(), e.getLineNumber(), e.getDirection(),
                    e.getAmountMinor(), e.getCurrency(), e.getDescription(), e.getEffectiveAt(), running));
        }

        return new StatementResponse(accountId, account.getCurrency(), windowStart, windowEnd,
                opening, running, statementLines);
    }

    private long openingBalance(String accountId, NormalBalance nb, Instant before) {
        Object[] row = entries.aggregateForAccountBetween(
                accountId, EntryDirection.DEBIT, EntryDirection.CREDIT, BEGINNING, before).get(0);
        long debit = ((Number) row[0]).longValue();
        long credit = ((Number) row[1]).longValue();
        return nb == NormalBalance.DEBIT ? (debit - credit) : (credit - debit);
    }

    private NormalBalance normalBalanceOf(Account account) {
        AccountType type = accountTypes.findById(account.getTypeCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Account " + account.getId() + " references unknown type " + account.getTypeCode()));
        return type.getNormalBalance();
    }
}
