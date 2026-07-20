package com.paymentprocessor.limit.service;

import com.paymentprocessor.limit.config.LimitProperties;
import com.paymentprocessor.limit.domain.entity.LimitConfiguration;
import com.paymentprocessor.limit.domain.entity.LimitReservation;
import com.paymentprocessor.limit.domain.entity.ReservationLine;
import com.paymentprocessor.limit.domain.entity.UsageCounter;
import com.paymentprocessor.limit.domain.enums.AuditAction;
import com.paymentprocessor.limit.domain.enums.EnforcementMode;
import com.paymentprocessor.limit.domain.enums.LimitDimension;
import com.paymentprocessor.limit.domain.enums.ReservationStatus;
import com.paymentprocessor.limit.domain.enums.TimeWindow;
import com.paymentprocessor.limit.dto.LimitViolationDto;
import com.paymentprocessor.limit.dto.ReservationResponse;
import com.paymentprocessor.limit.dto.ReserveRequest;
import com.paymentprocessor.limit.event.LimitEvent;
import com.paymentprocessor.limit.event.LimitEventPublisher;
import com.paymentprocessor.limit.exception.InvalidReservationStateException;
import com.paymentprocessor.limit.exception.LimitExceededException;
import com.paymentprocessor.limit.exception.ResourceNotFoundException;
import com.paymentprocessor.limit.repository.LimitReservationRepository;
import com.paymentprocessor.limit.repository.ReservationLineRepository;
import com.paymentprocessor.limit.repository.UsageCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the reserve → commit → release lifecycle described in the README.
 *
 * <p>Correctness under concurrency is guaranteed by taking a pessimistic write lock
 * ({@code SELECT … FOR UPDATE}) on each affected {@link UsageCounter} row before it
 * is read and mutated. Because the lock is held by the database it serialises
 * competing transactions across every service instance, preventing the race that
 * would otherwise allow two payments to each consume the last of a limit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final LimitResolver limitResolver;
    private final WindowResolver windowResolver;
    private final UsageCounterRepository usageCounterRepository;
    private final LimitReservationRepository reservationRepository;
    private final ReservationLineRepository reservationLineRepository;
    private final LimitEventPublisher eventPublisher;
    private final AuditService auditService;
    private final LimitProperties properties;

    // ------------------------------------------------------------------ reserve

    @Transactional
    public ReservationResponse reserve(ReserveRequest req) {
        // Idempotent replay: return the existing reservation unchanged.
        if (req.idempotencyKey() != null) {
            var existing = reservationRepository.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get(), List.of());
            }
        }

        List<LimitConfiguration> configs = limitResolver.resolve(
                req.customerId(), req.merchantId(), req.currency(), req.country());

        // Lock in a deterministic order (by config id) to avoid deadlocks.
        List<LimitConfiguration> ordered = configs.stream()
                .sorted(Comparator.comparing(c -> c.getId().toString()))
                .toList();

        Instant now = Instant.now();
        List<LimitViolationDto> hard = new ArrayList<>();
        List<LimitViolationDto> soft = new ArrayList<>();
        Map<UUID, UsageCounter> locked = new LinkedHashMap<>();
        Map<UUID, String> windowKeys = new LinkedHashMap<>();

        for (LimitConfiguration cfg : ordered) {
            BigDecimal requested = LimitEvaluationService.requested(cfg, req.amount());

            if (cfg.getTimeWindow() == TimeWindow.PER_TRANSACTION) {
                if (requested.compareTo(cfg.getThreshold()) > 0) {
                    classify(cfg, BigDecimal.ZERO, requested, hard, soft);
                }
                continue;
            }

            WindowResolver.Window window = windowResolver.resolve(
                    cfg.getTimeWindow(), cfg.getTimeZone(), now);
            UsageCounter counter = getOrCreateLockedCounter(cfg, window, now);
            locked.put(cfg.getId(), counter);
            windowKeys.put(cfg.getId(), window.key());

            BigDecimal used = LimitEvaluationService.usageFor(cfg, counter);
            if (used.add(requested).compareTo(cfg.getThreshold()) > 0) {
                classify(cfg, used, requested, hard, soft);
            }
        }

        if (!hard.isEmpty()) {
            publishExceeded(req, hard);
            auditService.record(AuditAction.LIMIT_EXCEEDED, entityRef(req),
                    req.transactionId(), "payment-service", hard);
            // Rolls back the transaction: any locks and tentative counter creates are undone.
            throw new LimitExceededException(hard);
        }

        // Passed all hard limits — persist the reservation and hold capacity.
        LimitReservation reservation = LimitReservation.builder()
                .transactionId(req.transactionId())
                .idempotencyKey(req.idempotencyKey())
                .customerId(req.customerId())
                .merchantId(req.merchantId())
                .currency(req.currency())
                .reservedAmount(req.amount())
                .committedAmount(BigDecimal.ZERO)
                .status(ReservationStatus.RESERVED)
                .expiresAt(now.plus(properties.getReservationTtlMinutes(), ChronoUnit.MINUTES))
                .createdAt(now)
                .updatedAt(now)
                .build();
        reservation = reservationRepository.save(reservation);

        List<ReservationLine> lines = new ArrayList<>();
        for (LimitConfiguration cfg : ordered) {
            UsageCounter counter = locked.get(cfg.getId());
            if (counter == null) {
                continue; // PER_TRANSACTION — nothing to hold
            }
            boolean amount = cfg.getDimension() == LimitDimension.AMOUNT;
            if (amount) {
                counter.setReservedAmount(counter.getReservedAmount().add(req.amount()));
            } else {
                counter.setReservedCount(counter.getReservedCount() + 1);
            }
            counter.setUpdatedAt(now);
            usageCounterRepository.save(counter);

            lines.add(ReservationLine.builder()
                    .reservationId(reservation.getId())
                    .limitConfigId(cfg.getId())
                    .usageCounterId(counter.getId())
                    .windowKey(windowKeys.get(cfg.getId()))
                    .heldAmount(amount ? req.amount() : BigDecimal.ZERO)
                    .heldCount(amount ? 0L : 1L)
                    .committedAmount(BigDecimal.ZERO)
                    .build());
        }
        reservationLineRepository.saveAll(lines);

        eventPublisher.publishReserved(new LimitEvent.LimitReserved(
                UUID.randomUUID().toString(), reservation.getId().toString(), req.transactionId(),
                req.customerId(), req.merchantId(), req.currency(), req.amount(),
                reservation.getExpiresAt(), now));
        auditService.record(AuditAction.LIMIT_RESERVED, entityRef(req),
                req.transactionId(), "payment-service", toResponse(reservation, soft));

        return toResponse(reservation, soft);
    }

    // ------------------------------------------------------------------ commit

    @Transactional
    public ReservationResponse commit(UUID reservationId, BigDecimal capturedAmount) {
        LimitReservation reservation = getReservation(reservationId);
        requireStatus(reservation, ReservationStatus.RESERVED, "commit");
        if (capturedAmount.compareTo(reservation.getReservedAmount()) > 0) {
            throw new InvalidReservationStateException(
                    "capturedAmount " + capturedAmount + " exceeds reserved amount "
                            + reservation.getReservedAmount());
        }
        Instant now = Instant.now();

        for (ReservationLine line : reservationLineRepository.findByReservationId(reservationId)) {
            UsageCounter counter = lockCounter(line.getUsageCounterId());
            if (line.getHeldAmount().signum() > 0) {
                // Move captured portion reserved -> committed; release the remainder.
                counter.setReservedAmount(nonNegative(
                        counter.getReservedAmount().subtract(line.getHeldAmount())));
                counter.setCommittedAmount(counter.getCommittedAmount().add(capturedAmount));
                line.setCommittedAmount(capturedAmount);
            }
            if (line.getHeldCount() > 0) {
                // The transaction occurred — it counts as one committed transaction.
                counter.setReservedCount(Math.max(0, counter.getReservedCount() - line.getHeldCount()));
                counter.setCommittedCount(counter.getCommittedCount() + line.getHeldCount());
            }
            counter.setUpdatedAt(now);
            usageCounterRepository.save(counter);
        }
        reservationLineRepository.saveAll(reservationLineRepository.findByReservationId(reservationId));

        reservation.setStatus(ReservationStatus.COMMITTED);
        reservation.setCommittedAmount(capturedAmount);
        reservation.setUpdatedAt(now);
        reservationRepository.save(reservation);

        BigDecimal released = reservation.getReservedAmount().subtract(capturedAmount);
        if (released.signum() > 0) {
            eventPublisher.publishReleased(new LimitEvent.LimitReleased(
                    UUID.randomUUID().toString(), reservation.getId().toString(),
                    reservation.getTransactionId(), "partial-capture-remainder", released, now));
        }
        auditService.record(AuditAction.LIMIT_COMMITTED, entityRef(reservation),
                reservation.getTransactionId(), "payment-service", toResponse(reservation, List.of()));
        return toResponse(reservation, List.of());
    }

    // ------------------------------------------------------------------ release

    @Transactional
    public ReservationResponse release(UUID reservationId, String reason) {
        LimitReservation reservation = getReservation(reservationId);
        if (reservation.getStatus() == ReservationStatus.RELEASED
                || reservation.getStatus() == ReservationStatus.EXPIRED) {
            return toResponse(reservation, List.of()); // idempotent
        }
        requireStatus(reservation, ReservationStatus.RESERVED, "release");
        return releaseInternal(reservation, ReservationStatus.RELEASED,
                reason == null ? "manual-release" : reason);
    }

    /** Used by the expiry sweeper; assumes the reservation row is already loaded. */
    @Transactional
    public void expire(UUID reservationId) {
        LimitReservation reservation = getReservation(reservationId);
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            return;
        }
        releaseInternal(reservation, ReservationStatus.EXPIRED, "reservation-expired");
    }

    private ReservationResponse releaseInternal(LimitReservation reservation,
                                                ReservationStatus finalStatus, String reason) {
        Instant now = Instant.now();
        BigDecimal totalReleased = BigDecimal.ZERO;
        for (ReservationLine line : reservationLineRepository.findByReservationId(reservation.getId())) {
            UsageCounter counter = lockCounter(line.getUsageCounterId());
            BigDecimal releaseAmount = line.getHeldAmount().subtract(line.getCommittedAmount());
            if (releaseAmount.signum() > 0) {
                counter.setReservedAmount(nonNegative(counter.getReservedAmount().subtract(releaseAmount)));
                totalReleased = totalReleased.add(releaseAmount);
            }
            if (line.getHeldCount() > 0) {
                counter.setReservedCount(Math.max(0, counter.getReservedCount() - line.getHeldCount()));
            }
            counter.setUpdatedAt(now);
            usageCounterRepository.save(counter);
        }
        reservation.setStatus(finalStatus);
        reservation.setUpdatedAt(now);
        reservationRepository.save(reservation);

        eventPublisher.publishReleased(new LimitEvent.LimitReleased(
                UUID.randomUUID().toString(), reservation.getId().toString(),
                reservation.getTransactionId(), reason, totalReleased, now));
        auditService.record(
                finalStatus == ReservationStatus.EXPIRED ? AuditAction.LIMIT_EXPIRED : AuditAction.LIMIT_RELEASED,
                entityRef(reservation), reservation.getTransactionId(), "payment-service",
                Map.of("reason", reason, "released", totalReleased.toPlainString()));
        return toResponse(reservation, List.of());
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public ReservationResponse getByReservationId(UUID reservationId) {
        return toResponse(getReservation(reservationId), List.of());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getByTransaction(String transactionId) {
        return reservationRepository.findByTransactionId(transactionId).stream()
                .map(r -> toResponse(r, List.of())).toList();
    }

    // ------------------------------------------------------------------ helpers

    private UsageCounter getOrCreateLockedCounter(LimitConfiguration cfg,
                                                  WindowResolver.Window window, Instant now) {
        return usageCounterRepository.lockByLimitConfigIdAndWindowKey(cfg.getId(), window.key())
                .orElseGet(() -> {
                    try {
                        return usageCounterRepository.saveAndFlush(UsageCounter.builder()
                                .limitConfigId(cfg.getId())
                                .windowKey(window.key())
                                .windowStart(window.start())
                                .updatedAt(now)
                                .build());
                    } catch (DataIntegrityViolationException race) {
                        // Concurrent create won — re-acquire the lock on the existing row.
                        return usageCounterRepository
                                .lockByLimitConfigIdAndWindowKey(cfg.getId(), window.key())
                                .orElseThrow(() -> race);
                    }
                });
    }

    private UsageCounter lockCounter(UUID usageCounterId) {
        UsageCounter counter = usageCounterRepository.findById(usageCounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usage counter not found: " + usageCounterId));
        return usageCounterRepository
                .lockByLimitConfigIdAndWindowKey(counter.getLimitConfigId(), counter.getWindowKey())
                .orElse(counter);
    }

    private void classify(LimitConfiguration cfg, BigDecimal used, BigDecimal requested,
                          List<LimitViolationDto> hard, List<LimitViolationDto> soft) {
        LimitViolationDto v = LimitEvaluationService.violation(cfg, used, requested);
        if (cfg.getEnforcement() == EnforcementMode.HARD) {
            hard.add(v);
        } else {
            soft.add(v);
        }
    }

    private LimitReservation getReservation(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    private void requireStatus(LimitReservation reservation, ReservationStatus expected, String op) {
        if (reservation.getStatus() != expected) {
            throw new InvalidReservationStateException(
                    "Cannot " + op + " reservation " + reservation.getId()
                            + " in status " + reservation.getStatus());
        }
    }

    private void publishExceeded(ReserveRequest req, List<LimitViolationDto> hard) {
        List<LimitEvent.Violation> violations = hard.stream()
                .map(v -> new LimitEvent.Violation(v.limitConfigId(), v.limitName(), v.dimension(),
                        v.timeWindow(), v.enforcement(), v.threshold(), v.attempted()))
                .toList();
        eventPublisher.publishExceeded(new LimitEvent.LimitExceeded(
                UUID.randomUUID().toString(), req.transactionId(), req.customerId(), req.merchantId(),
                req.currency(), "DECLINED", violations, Instant.now()));
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private static String entityRef(ReserveRequest req) {
        return req.customerId() != null ? "customer:" + req.customerId()
                : req.merchantId() != null ? "merchant:" + req.merchantId() : "unknown";
    }

    private static String entityRef(LimitReservation r) {
        return r.getCustomerId() != null ? "customer:" + r.getCustomerId()
                : r.getMerchantId() != null ? "merchant:" + r.getMerchantId() : "unknown";
    }

    private static ReservationResponse toResponse(LimitReservation r, List<LimitViolationDto> soft) {
        return new ReservationResponse(
                r.getId().toString(), r.getTransactionId(), r.getStatus(), r.getCurrency(),
                r.getReservedAmount(), r.getCommittedAmount(), r.getExpiresAt(), soft);
    }
}
