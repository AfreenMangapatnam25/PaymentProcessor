package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.NormalBalance;
import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.entity.AccountBalance;
import com.paymentprocessor.ledgerservice.entity.AccountType;
import com.paymentprocessor.ledgerservice.repository.AccountBalanceRepository;
import com.paymentprocessor.ledgerservice.repository.AccountRepository;
import com.paymentprocessor.ledgerservice.repository.AccountTypeRepository;
import com.paymentprocessor.ledgerservice.web.dto.BalanceResponse;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to real-time account balances.
 */
@Service
@Transactional(readOnly = true)
public class AccountBalanceService {

    private final AccountBalanceRepository balances;
    private final AccountRepository accounts;
    private final AccountTypeRepository accountTypes;

    public AccountBalanceService(AccountBalanceRepository balances,
                                 AccountRepository accounts,
                                 AccountTypeRepository accountTypes) {
        this.balances = balances;
        this.accounts = accounts;
        this.accountTypes = accountTypes;
    }

    public AccountBalance get(String accountId) {
        return balances.findById(accountId)
                .orElseThrow(() -> NotFoundException.of("Balance for account", accountId));
    }

    public BalanceResponse toResponse(AccountBalance balance) {
        NormalBalance nb = null;
        Account account = accounts.findById(balance.getAccountId()).orElse(null);
        if (account != null) {
            AccountType type = accountTypes.findById(account.getTypeCode()).orElse(null);
            nb = type != null ? type.getNormalBalance() : null;
        }
        return new BalanceResponse(
                balance.getAccountId(),
                balance.getCurrency(),
                nb,
                nz(balance.getPostedMinor()),
                nz(balance.getPendingMinor()),
                nz(balance.getHeldMinor()),
                nz(balance.getAvailableMinor()),
                nz(balance.getEntryHighWater()),
                balance.getVersion() == null ? 0 : balance.getVersion(),
                balance.getUpdatedAt());
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }
}
