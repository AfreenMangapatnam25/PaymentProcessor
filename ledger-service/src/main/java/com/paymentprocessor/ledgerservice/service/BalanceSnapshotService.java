package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import com.paymentprocessor.ledgerservice.domain.enums.NormalBalance;
import com.paymentprocessor.ledgerservice.domain.enums.SnapshotType;
import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.entity.AccountType;
import com.paymentprocessor.ledgerservice.entity.BalanceSnapshot;
import com.paymentprocessor.ledgerservice.entity.BalanceSnapshotId;
import com.paymentprocessor.ledgerservice.repository.AccountRepository;
import com.paymentprocessor.ledgerservice.repository.AccountTypeRepository;
import com.paymentprocessor.ledgerservice.repository.BalanceSnapshotRepository;
import com.paymentprocessor.ledgerservice.repository.EntryRepository;
import com.paymentprocessor.ledgerservice.web.dto.GenerateSnapshotRequest;
import com.paymentprocessor.ledgerservice.web.dto.SnapshotResponse;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates and reads immutable point-in-time balance snapshots. A snapshot for
 * a given (account, date) is written once; regeneration is a no-op that returns
 * the existing record, preserving immutability (README §Balance Snapshots).
 */
@Service
public class BalanceSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(BalanceSnapshotService.class);
    private static final Instant BEGINNING = Instant.EPOCH;

    private final BalanceSnapshotRepository snapshots;
    private final EntryRepository entries;
    private final AccountRepository accounts;
    private final AccountTypeRepository accountTypes;
    private final Clock clock;

    public BalanceSnapshotService(BalanceSnapshotRepository snapshots,
                                  EntryRepository entries,
                                  AccountRepository accounts,
                                  AccountTypeRepository accountTypes,
                                  Clock clock) {
        this.snapshots = snapshots;
        this.entries = entries;
        this.accounts = accounts;
        this.accountTypes = accountTypes;
        this.clock = clock;
    }

    @Transactional
    public List<BalanceSnapshot> generate(GenerateSnapshotRequest req) {
        LocalDate asOf = req.asOfDate();
        SnapshotType type = req.snapshotType() != null ? req.snapshotType() : SnapshotType.EOD;
        Instant dayStart = asOf.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant asOfEnd = asOf.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = Instant.now(clock);

        List<Account> scope = (req.accountIds() == null || req.accountIds().isEmpty())
                ? accounts.findAll()
                : accounts.findAllById(req.accountIds());

        List<BalanceSnapshot> result = new ArrayList<>();
        for (Account account : scope) {
            BalanceSnapshotId id = snapshotId(account.getId(), asOf);
            var existing = snapshots.findById(id);
            if (existing.isPresent()) {
                result.add(existing.get());
                continue;
            }

            NormalBalance nb = normalBalanceOf(account);
            long[] closing = aggregate(account.getId(), asOfEnd);   // [debit, credit, count]
            long[] opening = aggregate(account.getId(), dayStart);
            Object[] closingRaw = entries.aggregateForAccountBetween(
                    account.getId(), EntryDirection.DEBIT, EntryDirection.CREDIT, BEGINNING, asOfEnd).get(0);

            long closingPosted = normalSide(nb, closing[0], closing[1]);
            long openingPosted = normalSide(nb, opening[0], opening[1]);
            long dayDebit = closing[0] - opening[0];
            long dayCredit = closing[1] - opening[1];
            long dayCount = closing[2] - opening[2];

            BalanceSnapshot snapshot = new BalanceSnapshot();
            snapshot.setAccountId(account.getId());
            snapshot.setAsOfDate(asOf);
            snapshot.setSnapshotType(type);
            snapshot.setOpeningMinor(openingPosted);
            snapshot.setDebitMinor(dayDebit);
            snapshot.setCreditMinor(dayCredit);
            snapshot.setPostedMinor(closingPosted);
            snapshot.setEntryCount(dayCount);
            snapshot.setEntryHighWater(((Number) closingRaw[3]).longValue());
            snapshot.setLastEntryAt((Instant) closingRaw[4]);
            snapshot.setCreatedAt(now);
            snapshots.save(snapshot);
            result.add(snapshot);
        }
        log.info("Generated {} balance snapshots as of {}", result.size(), asOf);
        return result;
    }

    @Transactional(readOnly = true)
    public List<BalanceSnapshot> forAccount(String accountId) {
        if (!accounts.existsById(accountId)) {
            throw NotFoundException.of("Account", accountId);
        }
        return snapshots.findByAccountIdOrderByAsOfDateAsc(accountId);
    }

    @Transactional(readOnly = true)
    public List<BalanceSnapshot> forDate(LocalDate date) {
        return snapshots.findByAsOfDate(date);
    }

    /** Returns [debit, credit, count] for all entries with effectiveAt <= upTo. */
    private long[] aggregate(String accountId, Instant upTo) {
        Object[] row = entries.aggregateForAccountBetween(
                accountId, EntryDirection.DEBIT, EntryDirection.CREDIT, BEGINNING, upTo).get(0);
        return new long[]{
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue()
        };
    }

    private static long normalSide(NormalBalance nb, long debit, long credit) {
        return nb == NormalBalance.DEBIT ? (debit - credit) : (credit - debit);
    }

    private NormalBalance normalBalanceOf(Account account) {
        AccountType type = accountTypes.findById(account.getTypeCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Account " + account.getId() + " references unknown type " + account.getTypeCode()));
        return type.getNormalBalance();
    }

    private static BalanceSnapshotId snapshotId(String accountId, LocalDate asOf) {
        BalanceSnapshotId id = new BalanceSnapshotId();
        id.setAccountId(accountId);
        id.setAsOfDate(asOf);
        return id;
    }

    public static SnapshotResponse toResponse(BalanceSnapshot s) {
        return new SnapshotResponse(
                s.getAccountId(),
                s.getAsOfDate(),
                s.getSnapshotType(),
                s.getOpeningMinor(),
                s.getDebitMinor(),
                s.getCreditMinor(),
                s.getPostedMinor(),
                s.getEntryCount(),
                s.getEntryHighWater(),
                s.getLastEntryAt(),
                s.getCreatedAt());
    }
}
