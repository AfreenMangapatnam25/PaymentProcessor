package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.AccountStatus;
import com.paymentprocessor.ledgerservice.domain.enums.NormalBalance;
import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.entity.AccountBalance;
import com.paymentprocessor.ledgerservice.entity.AccountType;
import com.paymentprocessor.ledgerservice.repository.AccountBalanceRepository;
import com.paymentprocessor.ledgerservice.repository.AccountRepository;
import com.paymentprocessor.ledgerservice.repository.AccountTypeRepository;
import com.paymentprocessor.ledgerservice.repository.CurrencyRepository;
import com.paymentprocessor.ledgerservice.support.Ids;
import com.paymentprocessor.ledgerservice.web.dto.AccountResponse;
import com.paymentprocessor.ledgerservice.web.dto.CreateAccountRequest;
import com.paymentprocessor.ledgerservice.web.error.ConflictException;
import com.paymentprocessor.ledgerservice.web.error.InvalidRequestException;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the chart of accounts. Accounts may be created and deactivated but,
 * like all ledger data, their history is never destroyed.
 */
@Service
public class AccountService {

    private final AccountRepository accounts;
    private final AccountTypeRepository accountTypes;
    private final CurrencyRepository currencies;
    private final AccountBalanceRepository balances;
    private final Clock clock;

    public AccountService(AccountRepository accounts,
                          AccountTypeRepository accountTypes,
                          CurrencyRepository currencies,
                          AccountBalanceRepository balances,
                          Clock clock) {
        this.accounts = accounts;
        this.accountTypes = accountTypes;
        this.currencies = currencies;
        this.balances = balances;
        this.clock = clock;
    }

    @Transactional
    public Account create(CreateAccountRequest req) {
        AccountType type = accountTypes.findById(req.typeCode())
                .orElseThrow(() -> new InvalidRequestException("Unknown account type: " + req.typeCode()));
        if (!currencies.existsById(req.currency())) {
            throw new InvalidRequestException("Unsupported currency: " + req.currency());
        }
        if (accounts.existsByAccountCode(req.accountCode())) {
            throw new ConflictException("ACCOUNT_CODE_EXISTS",
                    "An account already exists with code " + req.accountCode());
        }
        String id = (req.id() != null && !req.id().isBlank()) ? req.id() : Ids.accountId();
        if (accounts.existsById(id)) {
            throw new ConflictException("ACCOUNT_EXISTS", "An account already exists with id " + id);
        }
        if (req.parentAccountId() != null && !accounts.existsById(req.parentAccountId())) {
            throw new InvalidRequestException("Unknown parent account: " + req.parentAccountId());
        }

        Instant now = Instant.now(clock);
        Account account = new Account();
        account.setId(id);
        account.setAccountCode(req.accountCode());
        account.setName(req.name());
        account.setTypeCode(type.getCode());
        account.setCurrency(req.currency());
        account.setOwnerType(req.ownerType());
        account.setOwnerId(req.ownerId());
        account.setParentAccountId(req.parentAccountId());
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(now);
        accounts.save(account);

        AccountBalance balance = new AccountBalance();
        balance.setAccountId(id);
        balance.setCurrency(req.currency());
        balance.setPostedMinor(0L);
        balance.setPendingMinor(0L);
        balance.setHeldMinor(0L);
        balance.setAvailableMinor(0L);
        balance.setEntryHighWater(0L);
        balance.setUpdatedAt(now);
        balances.save(balance);

        return account;
    }

    @Transactional(readOnly = true)
    public Account get(String id) {
        return accounts.findById(id).orElseThrow(() -> NotFoundException.of("Account", id));
    }

    @Transactional(readOnly = true)
    public List<Account> list() {
        return accounts.findAll();
    }

    @Transactional
    public Account deactivate(String id) {
        Account account = get(id);
        account.setStatus(AccountStatus.INACTIVE);
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public NormalBalance normalBalanceOf(Account account) {
        AccountType type = accountTypes.findById(account.getTypeCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Account " + account.getId() + " references unknown type " + account.getTypeCode()));
        return type.getNormalBalance();
    }

    @Transactional(readOnly = true)
    public AccountResponse toResponse(Account account) {
        AccountType type = accountTypes.findById(account.getTypeCode()).orElse(null);
        return new AccountResponse(
                account.getId(),
                account.getAccountCode(),
                account.getName(),
                account.getOwnerType(),
                account.getOwnerId(),
                account.getTypeCode(),
                type != null ? type.getClassification() : null,
                type != null ? type.getNormalBalance() : null,
                account.getCurrency(),
                account.getParentAccountId(),
                account.getStatus(),
                account.getCreatedAt());
    }
}
