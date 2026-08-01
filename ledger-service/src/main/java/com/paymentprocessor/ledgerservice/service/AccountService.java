package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.AccountPurpose;
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
import com.paymentprocessor.ledgerservice.web.dto.ProvisionMerchantAccountsRequest;
import com.paymentprocessor.ledgerservice.web.dto.ProvisionMerchantAccountsResponse;
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

    @Transactional(readOnly = true)
    public List<Account> list(String ownerType, String ownerId, String accountCode) {
        if (accountCode != null && !accountCode.isBlank()) {
            return accounts.findByAccountCode(accountCode).stream().toList();
        }
        if (ownerType != null && !ownerType.isBlank() && ownerId != null && !ownerId.isBlank()) {
            return accounts.findByOwnerTypeAndOwnerId(ownerType, ownerId);
        }
        return accounts.findAll();
    }

    @Transactional
    public ProvisionMerchantAccountsResponse provisionMerchantAccounts(ProvisionMerchantAccountsRequest req) {
        String liabilityId = merchantAccountId(req.merchantId(), "settlement_liability");
        String reserveId = merchantAccountId(req.merchantId(), "reserve");
        ensureMerchantAccount(liabilityId, "merchant." + req.merchantId() + ".settlement_liability",
                "Merchant settlement liability", "LIABILITY", req.currency(), req.merchantId(), "2102");
        ensureMerchantAccount(reserveId, "merchant." + req.merchantId() + ".reserve",
                "Merchant rolling reserve", "LIABILITY", req.currency(), req.merchantId(), "1104");
        return new ProvisionMerchantAccountsResponse(liabilityId, reserveId);
    }

    @Transactional
    public Account resolve(AccountPurpose purpose, String ownerId, String currency) {
        String resolvedCurrency = (currency == null || currency.isBlank()) ? "USD" : currency;
        return switch (purpose) {
            case PLATFORM_CASH -> requireActive("platform:cash");
            case PLATFORM_FEE_REVENUE -> requireActive("platform:fee_revenue");
            case PLATFORM_PAYOUT_PAYABLE -> requireActive("platform:payout_payable");
            case PLATFORM_ADJUSTMENT_EXPENSE -> requireActive("platform:adjustment_expense");
            case PLATFORM_CHARGEBACK_CLEARING -> requireActive("platform:chargeback_clearing");
            case PLATFORM_FEE_EXPENSE -> requireActive("platform:fee_expense");
            case MERCHANT_SETTLEMENT_LIABILITY -> requireActive(resolveMerchantAccountId(
                    ownerId, resolvedCurrency, true));
            case MERCHANT_RESERVE -> requireActive(resolveMerchantAccountId(
                    ownerId, resolvedCurrency, false));
        };
    }

    private String resolveMerchantAccountId(String ownerId, String currency, boolean liability) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new InvalidRequestException("ownerId is required for merchant account purposes");
        }
        ProvisionMerchantAccountsResponse provisioned =
                provisionMerchantAccounts(new ProvisionMerchantAccountsRequest(ownerId, currency));
        return liability ? provisioned.settlementLiabilityAccountId() : provisioned.reserveAccountId();
    }

    private Account requireActive(String accountId) {
        Account account = get(accountId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidRequestException("Account " + accountId + " is not active");
        }
        return account;
    }

    private void ensureMerchantAccount(String id, String accountCode, String name, String typeCode,
                                       String currency, String merchantId, String parentAccountId) {
        if (accounts.existsById(id)) {
            return;
        }
        create(new CreateAccountRequest(id, accountCode, name, typeCode, currency,
                "MERCHANT", merchantId, parentAccountId));
    }

    private static String merchantAccountId(String merchantId, String suffix) {
        return "merchant:" + merchantId + ":" + suffix;
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
