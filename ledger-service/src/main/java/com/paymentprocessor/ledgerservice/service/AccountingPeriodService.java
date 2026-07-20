package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.domain.enums.PeriodState;
import com.paymentprocessor.ledgerservice.entity.AccountingPeriod;
import com.paymentprocessor.ledgerservice.repository.AccountingPeriodRepository;
import com.paymentprocessor.ledgerservice.support.Ids;
import com.paymentprocessor.ledgerservice.web.dto.CreatePeriodRequest;
import com.paymentprocessor.ledgerservice.web.error.ConflictException;
import com.paymentprocessor.ledgerservice.web.error.InvalidRequestException;
import com.paymentprocessor.ledgerservice.web.error.NotFoundException;
import com.paymentprocessor.ledgerservice.web.error.UnprocessableException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages accounting periods and enforces that postings only land in periods
 * that accept them.
 */
@Service
public class AccountingPeriodService {

    private final AccountingPeriodRepository repository;
    private final Clock clock;

    public AccountingPeriodService(AccountingPeriodRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public AccountingPeriod create(CreatePeriodRequest req) {
        if (req.endDate().isBefore(req.startDate())) {
            throw new InvalidRequestException("endDate must not be before startDate");
        }
        if (repository.findByCode(req.code()).isPresent()) {
            throw new ConflictException("PERIOD_CODE_EXISTS", "A period already exists with code " + req.code());
        }
        AccountingPeriod period = new AccountingPeriod();
        period.setId(Ids.periodId(req.code()));
        period.setCode(req.code());
        period.setPeriodType(req.periodType());
        period.setStartDate(req.startDate());
        period.setEndDate(req.endDate());
        period.setState(PeriodState.OPEN);
        period.setCreatedAt(Instant.now(clock));
        return repository.save(period);
    }

    @Transactional(readOnly = true)
    public AccountingPeriod get(String id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Accounting period", id));
    }

    @Transactional(readOnly = true)
    public List<AccountingPeriod> list() {
        return repository.findAll();
    }

    @Transactional
    public AccountingPeriod changeState(String id, PeriodState newState) {
        AccountingPeriod period = get(id);
        period.setState(newState);
        if (newState == PeriodState.CLOSED || newState == PeriodState.LOCKED) {
            period.setClosedAt(Instant.now(clock));
        } else {
            period.setClosedAt(null);
        }
        return repository.save(period);
    }

    /**
     * Resolve the period a posting with the given effective instant belongs to and
     * verify it accepts postings. Returns the period id, or {@code null} when no
     * period covers the date (posting is permitted without a period).
     *
     * @throws UnprocessableException if a covering period exists but is CLOSED/LOCKED.
     */
    @Transactional(readOnly = true)
    public String resolvePostablePeriodId(Instant effectiveAt) {
        LocalDate date = effectiveAt.atZone(ZoneOffset.UTC).toLocalDate();
        List<AccountingPeriod> covering =
                repository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        if (covering.isEmpty()) {
            return null;
        }
        // Prefer a period that accepts postings (e.g. a narrow open period over a wider locked one).
        AccountingPeriod postable = covering.stream()
                .filter(p -> p.getState().acceptsPostings())
                .findFirst()
                .orElse(null);
        if (postable != null) {
            return postable.getId();
        }
        AccountingPeriod blocking = covering.get(0);
        throw new UnprocessableException("PERIOD_CLOSED",
                "Accounting period " + blocking.getCode() + " is " + blocking.getState()
                        + " and does not accept postings for " + date);
    }
}
