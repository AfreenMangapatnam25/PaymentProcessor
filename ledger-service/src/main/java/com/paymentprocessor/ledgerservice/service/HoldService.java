package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.HoldStatus;
import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.entity.AccountBalance;
import com.paymentprocessor.ledgerservice.entity.Hold;
import com.paymentprocessor.ledgerservice.event.BalanceUpdatedEvent;
import com.paymentprocessor.ledgerservice.repository.AccountBalanceRepository;
import com.paymentprocessor.ledgerservice.repository.AccountRepository;
import com.paymentprocessor.ledgerservice.repository.HoldRepository;
import com.paymentprocessor.ledgerservice.support.Ids;
import com.paymentprocessor.ledgerservice.web.dto.HoldResponse;
import com.paymentprocessor.ledgerservice.web.dto.PlaceHoldRequest;
import com.paymentprocessor.ledgerservice.web.error.ConflictException;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import com.paymentprocessor.ledgerservice.web.error.UnprocessableException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places and releases holds (reservations) against an account's available
 * balance. Holds reduce {@code availableMinor} without altering {@code postedMinor}.
 */
@Service
public class HoldService {

    private static final Logger log = LoggerFactory.getLogger(HoldService.class);

    private final HoldRepository holds;
    private final AccountBalanceRepository balances;
    private final AccountRepository accounts;
    private final OutboxService outbox;
    private final Clock clock;

    public HoldService(HoldRepository holds,
                       AccountBalanceRepository balances,
                       AccountRepository accounts,
                       OutboxService outbox,
                       Clock clock) {
        this.holds = holds;
        this.balances = balances;
        this.accounts = accounts;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public Hold place(PlaceHoldRequest req) {
        Account account = accounts.findById(req.accountId())
                .orElseThrow(() -> NotFoundException.of("Account", req.accountId()));
        if (!account.isActive()) {
            throw new UnprocessableException("ACCOUNT_INACTIVE", "Account " + account.getId() + " is not active");
        }
        String currency = req.currency() != null ? req.currency() : account.getCurrency();
        if (!currency.equals(account.getCurrency())) {
            throw new UnprocessableException("CURRENCY_MISMATCH",
                    "Hold currency " + currency + " does not match account currency " + account.getCurrency());
        }

        Instant now = Instant.now(clock);
        AccountBalance balance = balances.findByIdForUpdate(req.accountId())
                .orElseThrow(() -> NotFoundException.of("Balance for account", req.accountId()));
        long available = nz(balance.getAvailableMinor());
        if (available < req.amountMinor()) {
            throw new UnprocessableException("INSUFFICIENT_AVAILABLE_BALANCE",
                    "Available balance " + available + " is less than requested hold " + req.amountMinor());
        }
        balance.setHeldMinor(nz(balance.getHeldMinor()) + req.amountMinor());
        balance.recomputeAvailable();
        balance.setUpdatedAt(now);
        balances.save(balance);

        Hold hold = new Hold();
        hold.setId(Ids.holdId());
        hold.setAccountId(req.accountId());
        hold.setAmountMinor(req.amountMinor());
        hold.setCurrency(currency);
        hold.setReason(req.reason());
        hold.setStatus(HoldStatus.ACTIVE);
        hold.setExternalRef(req.externalRef());
        hold.setExpiresAt(req.expiresAt());
        hold.setCreatedAt(now);
        holds.save(hold);

        publishBalance(balance, now);
        log.info("Placed hold {} on account {} for {} minor", hold.getId(), req.accountId(), req.amountMinor());
        return hold;
    }

    @Transactional
    public Hold release(String holdId) {
        Hold hold = holds.findById(holdId).orElseThrow(() -> NotFoundException.of("Hold", holdId));
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new ConflictException("HOLD_NOT_ACTIVE",
                    "Hold " + holdId + " is " + hold.getStatus() + " and cannot be released");
        }
        Instant now = Instant.now(clock);
        AccountBalance balance = balances.findByIdForUpdate(hold.getAccountId())
                .orElseThrow(() -> NotFoundException.of("Balance for account", hold.getAccountId()));
        balance.setHeldMinor(Math.max(0L, nz(balance.getHeldMinor()) - hold.getAmountMinor()));
        balance.recomputeAvailable();
        balance.setUpdatedAt(now);
        balances.save(balance);

        hold.setStatus(HoldStatus.RELEASED);
        hold.setReleasedAt(now);
        holds.save(hold);

        publishBalance(balance, now);
        log.info("Released hold {} on account {}", holdId, hold.getAccountId());
        return hold;
    }

    @Transactional(readOnly = true)
    public Hold get(String holdId) {
        return holds.findById(holdId).orElseThrow(() -> NotFoundException.of("Hold", holdId));
    }

    @Transactional(readOnly = true)
    public List<Hold> listForAccount(String accountId) {
        return holds.findByAccountId(accountId);
    }

    private void publishBalance(AccountBalance balance, Instant now) {
        outbox.append("Account", balance.getAccountId(), BalanceUpdatedEvent.TYPE, new BalanceUpdatedEvent(
                balance.getAccountId(), balance.getCurrency(), nz(balance.getPostedMinor()),
                nz(balance.getHeldMinor()), nz(balance.getAvailableMinor()),
                nz(balance.getEntryHighWater()), now));
    }

    public static HoldResponse toResponse(Hold hold) {
        return new HoldResponse(
                hold.getId(),
                hold.getAccountId(),
                hold.getAmountMinor(),
                hold.getCurrency(),
                hold.getReason(),
                hold.getStatus(),
                hold.getExternalRef(),
                hold.getExpiresAt(),
                hold.getCreatedAt(),
                hold.getReleasedAt());
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }
}
