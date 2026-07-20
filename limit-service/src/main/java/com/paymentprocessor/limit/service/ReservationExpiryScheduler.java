package com.paymentprocessor.limit.service;

import com.paymentprocessor.limit.domain.entity.LimitReservation;
import com.paymentprocessor.limit.domain.enums.ReservationStatus;
import com.paymentprocessor.limit.repository.LimitReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically releases reservations that have passed their expiry, returning the
 * held capacity to the available pool (README "Reservation expires → automatic
 * release"). Each reservation is expired in its own transaction so a single
 * failure does not block the rest of the batch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryScheduler {

    private static final int BATCH_SIZE = 200;

    private final LimitReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${limit.expiry-sweep-interval-ms:60000}")
    public void sweepExpiredReservations() {
        List<LimitReservation> expired = reservationRepository.findExpired(
                ReservationStatus.RESERVED, Instant.now(), PageRequest.of(0, BATCH_SIZE));
        if (expired.isEmpty()) {
            return;
        }
        int released = 0;
        for (LimitReservation reservation : expired) {
            try {
                reservationService.expire(reservation.getId());
                released++;
            } catch (Exception ex) {
                log.warn("Failed to expire reservation {}: {}", reservation.getId(), ex.getMessage());
            }
        }
        log.info("Expiry sweep released {} of {} candidate reservation(s)", released, expired.size());
    }
}
