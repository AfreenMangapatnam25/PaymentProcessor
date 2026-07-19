package com.paymentprocessor.limit.service;

import com.paymentprocessor.limit.domain.entity.LimitConfiguration;
import com.paymentprocessor.limit.domain.entity.UsageCounter;
import com.paymentprocessor.limit.domain.enums.EnforcementMode;
import com.paymentprocessor.limit.domain.enums.LimitDecision;
import com.paymentprocessor.limit.domain.enums.LimitDimension;
import com.paymentprocessor.limit.domain.enums.TimeWindow;
import com.paymentprocessor.limit.dto.LimitCheckRequest;
import com.paymentprocessor.limit.dto.LimitCheckResponse;
import com.paymentprocessor.limit.dto.LimitViolationDto;
import com.paymentprocessor.limit.dto.UsageDto;
import com.paymentprocessor.limit.repository.UsageCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only evaluation of a transaction against the applicable limits. Performs no
 * locking and reserves nothing; used for pre-checks and usage dashboards. The
 * authoritative check happens inside {@link ReservationService#reserve} under a
 * pessimistic lock.
 */
@Service
@RequiredArgsConstructor
public class LimitEvaluationService {

    private final LimitResolver limitResolver;
    private final WindowResolver windowResolver;
    private final UsageCounterRepository usageCounterRepository;

    @Transactional(readOnly = true)
    public LimitCheckResponse check(LimitCheckRequest req) {
        List<LimitConfiguration> configs = limitResolver.resolve(
                req.customerId(), req.merchantId(), req.currency(), req.country());

        List<LimitViolationDto> hard = new ArrayList<>();
        List<LimitViolationDto> soft = new ArrayList<>();
        Instant now = Instant.now();

        for (LimitConfiguration cfg : configs) {
            BigDecimal used = currentUsage(cfg, now);
            BigDecimal requested = requested(cfg, req.amount());
            BigDecimal projected = used.add(requested);

            if (projected.compareTo(cfg.getThreshold()) > 0) {
                LimitViolationDto v = violation(cfg, used, requested);
                if (cfg.getEnforcement() == EnforcementMode.HARD) {
                    hard.add(v);
                } else {
                    soft.add(v);
                }
            }
        }

        LimitDecision decision = hard.isEmpty()
                ? (soft.isEmpty() ? LimitDecision.APPROVED : LimitDecision.FLAGGED)
                : LimitDecision.DECLINED;
        return new LimitCheckResponse(decision, hard, soft, configs.size());
    }

    @Transactional(readOnly = true)
    public List<UsageDto> usage(String customerId, String merchantId, String currency) {
        List<LimitConfiguration> configs = limitResolver.resolve(customerId, merchantId, currency, null);
        Instant now = Instant.now();
        List<UsageDto> out = new ArrayList<>();
        for (LimitConfiguration cfg : configs) {
            if (cfg.getTimeWindow() == TimeWindow.PER_TRANSACTION) {
                continue; // stateless — no running usage
            }
            WindowResolver.Window w = windowResolver.resolve(cfg.getTimeWindow(), cfg.getTimeZone(), now);
            BigDecimal used = currentUsage(cfg, now);
            BigDecimal remaining = cfg.getThreshold().subtract(used).max(BigDecimal.ZERO);
            out.add(new UsageDto(
                    cfg.getId().toString(), cfg.getName(), cfg.getDimension().name(),
                    cfg.getTimeWindow().name(), w.key(), cfg.getThreshold(), used, remaining,
                    cfg.getCurrency(), cfg.getEnforcement().name()));
        }
        return out;
    }

    private BigDecimal currentUsage(LimitConfiguration cfg, Instant now) {
        if (cfg.getTimeWindow() == TimeWindow.PER_TRANSACTION) {
            return BigDecimal.ZERO;
        }
        WindowResolver.Window w = windowResolver.resolve(cfg.getTimeWindow(), cfg.getTimeZone(), now);
        return usageCounterRepository.findByLimitConfigIdAndWindowKey(cfg.getId(), w.key())
                .map(c -> cfg.getDimension() == LimitDimension.AMOUNT
                        ? c.usedAmount()
                        : BigDecimal.valueOf(c.usedCount()))
                .orElse(BigDecimal.ZERO);
    }

    static BigDecimal requested(LimitConfiguration cfg, BigDecimal amount) {
        return cfg.getDimension() == LimitDimension.AMOUNT ? amount : BigDecimal.ONE;
    }

    static LimitViolationDto violation(LimitConfiguration cfg, BigDecimal used, BigDecimal requested) {
        String msg = "%s limit of %s exceeded (current usage %s, attempted +%s)".formatted(
                cfg.getTimeWindow(), cfg.getThreshold().toPlainString(),
                used.toPlainString(), requested.toPlainString());
        return new LimitViolationDto(
                cfg.getId().toString(), cfg.getName(), cfg.getDimension().name(),
                cfg.getTimeWindow().name(), cfg.getEnforcement().name(),
                cfg.getThreshold(), used, requested, msg);
    }

    static BigDecimal usageFor(LimitConfiguration cfg, UsageCounter counter) {
        return cfg.getDimension() == LimitDimension.AMOUNT
                ? counter.usedAmount() : BigDecimal.valueOf(counter.usedCount());
    }
}
