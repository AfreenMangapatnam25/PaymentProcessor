package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.AccountClassification;
import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.entity.AccountType;
import com.paymentprocessor.ledgerservice.repository.AccountRepository;
import com.paymentprocessor.ledgerservice.repository.AccountTypeRepository;
import com.paymentprocessor.ledgerservice.repository.EntryRepository;
import com.paymentprocessor.ledgerservice.web.dto.TrialBalanceLine;
import com.paymentprocessor.ledgerservice.web.dto.TrialBalanceResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces a trial balance: every account's net balance placed on its natural
 * side. For an internally consistent ledger the debit and credit columns are
 * always equal (README §Trial Balance).
 */
@Service
@Transactional(readOnly = true)
public class TrialBalanceService {

    private final EntryRepository entries;
    private final AccountRepository accounts;
    private final AccountTypeRepository accountTypes;
    private final Clock clock;

    public TrialBalanceService(EntryRepository entries,
                               AccountRepository accounts,
                               AccountTypeRepository accountTypes,
                               Clock clock) {
        this.entries = entries;
        this.accounts = accounts;
        this.accountTypes = accountTypes;
        this.clock = clock;
    }

    public TrialBalanceResponse generate(Instant asOf) {
        Instant effectiveAsOf = asOf != null ? asOf : Instant.now(clock);
        List<Object[]> rows = entries.aggregateByAccountUpTo(
                EntryDirection.DEBIT, EntryDirection.CREDIT, effectiveAsOf);

        Map<String, AccountType> typeCache = new HashMap<>();
        List<TrialBalanceLine> lines = new ArrayList<>();
        long totalDebit = 0;
        long totalCredit = 0;

        for (Object[] row : rows) {
            String accountId = (String) row[0];
            long debit = ((Number) row[1]).longValue();
            long credit = ((Number) row[2]).longValue();
            long net = debit - credit; // debit-positive

            Account account = accounts.findById(accountId).orElse(null);
            AccountClassification classification = null;
            String code = accountId;
            String name = accountId;
            if (account != null) {
                code = account.getAccountCode();
                name = account.getName();
                AccountType type = typeCache.computeIfAbsent(account.getTypeCode(),
                        tc -> accountTypes.findById(tc).orElse(null));
                classification = type != null ? type.getClassification() : null;
            }

            long debitColumn = net > 0 ? net : 0;
            long creditColumn = net < 0 ? -net : 0;
            if (debitColumn == 0 && creditColumn == 0) {
                continue; // zero-balance accounts are omitted
            }
            totalDebit += debitColumn;
            totalCredit += creditColumn;
            lines.add(new TrialBalanceLine(code, name, classification, debitColumn, creditColumn));
        }

        lines.sort(Comparator.comparing(TrialBalanceLine::accountCode,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return new TrialBalanceResponse(effectiveAsOf, lines, totalDebit, totalCredit,
                totalDebit == totalCredit);
    }
}
